package com.orasaka.core.pipeline;

import com.orasaka.core.engine.CoreProperties;
import com.orasaka.core.support.InternalChatRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

public final class PipelineInterceptors {
  private PipelineInterceptors() {}

  static Optional<String> extractResponseText(ChatResponse response) {
    return Optional.ofNullable(response)
        .map(ChatResponse::getResult)
        .map(result -> result.getOutput())
        .map(output -> output.getText())
        .map(String::strip)
        .filter(text -> !text.isBlank());
  }
}

@Component
class UserContextResolver implements PromptInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(UserContextResolver.class);

  @Override
  public PromptContext intercept(PromptContext context) {
    logger.debug("Resolving user security context details...");
    var securityData = SecurityContextUtil.extractSecurityMetadata();
    if (securityData.isEmpty()) {
      return context;
    }
    var newUserMetadata = new HashMap<>(context.userMetadata());
    newUserMetadata.putAll(securityData);
    logger.debug(
        "Successfully enriched user metadata with security claims: {}", securityData.keySet());
    return context.withUserMetadata(Map.copyOf(newUserMetadata));
  }

  @Override
  public int getOrder() {
    return 1;
  }
}

@Component
class SystemContextInjector implements PromptInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(SystemContextInjector.class);
  private final List<SystemContextProvider> providers;

  public SystemContextInjector(List<SystemContextProvider> providers) {
    this.providers = List.copyOf(Objects.requireNonNullElse(providers, List.of()));
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    logger.debug("Injecting system context signals. Provider count: {}", providers.size());
    var newSystemMetadata = new HashMap<>(context.systemMetadata());

    for (SystemContextProvider provider : providers) {
      try {
        Map<String, Object> data = provider.getSystemContext();
        if (data != null) newSystemMetadata.putAll(data);
      } catch (Exception e) {
        logger.error("Error invoking SystemContextProvider: {}", provider.getClass().getName(), e);
      }
    }
    return context.withSystemMetadata(Map.copyOf(newSystemMetadata));
  }

  @Override
  public int getOrder() {
    return 2;
  }
}

@Component
class RefinerInterceptor implements PromptInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(RefinerInterceptor.class);

  private final Map<String, ChatModel> chatModels;
  private final CoreProperties properties;

  @Value("classpath:/prompts/system-refinement.st")
  private Resource systemRefinementResource;

  @Value("classpath:/prompts/context-envelope.st")
  private Resource contextEnvelopeResource;

  public RefinerInterceptor(Map<String, ChatModel> chatModels, CoreProperties properties) {
    this.chatModels = Map.copyOf(Objects.requireNonNullElse(chatModels, Map.of()));
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    if (!isRefinementEnabled()) return context;

    String provider = resolveProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      logger.warn("Refinement provider '{}' is not registered.", provider);
      return context;
    }

    Prompt prompt = buildRefinementPrompt(provider, context);
    return executeRefinement(model, prompt, context);
  }

  private boolean isRefinementEnabled() {
    return Optional.ofNullable(properties.orchestration())
        .map(CoreProperties.OrchestrationConfig::refiner)
        .map(CoreProperties.InterceptorConfig::enabled)
        .orElse(false);
  }

  private String resolveProvider() {
    return Optional.ofNullable(properties.orchestration())
        .map(CoreProperties.OrchestrationConfig::refiner)
        .map(CoreProperties.InterceptorConfig::provider)
        .orElse(properties.defaultProvider());
  }

  private Prompt buildRefinementPrompt(String provider, PromptContext context) {
    var sysMsg = new SystemPromptTemplate(systemRefinementResource).createMessage();
    var userMsg =
        new PromptTemplate(contextEnvelopeResource)
            .createMessage(
                Map.of(
                    "userMetadata", context.userMetadata().toString(),
                    "systemMetadata", context.systemMetadata().toString(),
                    "rawQuery", context.rawUserQuery()));

    var opt = properties.orchestration().refiner();
    ChatOptions options = PipelineOptionsBuilder.build(provider, opt.model(), opt.temperature());
    return new Prompt(List.of(sysMsg, userMsg), options);
  }

  private PromptContext executeRefinement(ChatModel model, Prompt prompt, PromptContext context) {
    try {
      return PipelineInterceptors.extractResponseText(model.call(prompt))
          .filter(refined -> !refined.isBlank())
          .map(
              refined -> {
                logger.debug("Refinement generated: {}", refined);
                return context.withRefinedPrompt(refined);
              })
          .orElse(context);
    } catch (Exception e) {
      logger.error("Failed to refine prompt.", e);
      return context;
    }
  }

  @Override
  public int getOrder() {
    return 3;
  }
}

@Component
class RouterInterceptor implements PromptInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(RouterInterceptor.class);

  private final Map<String, ChatModel> chatModels;
  private final CoreProperties properties;

  @Value("classpath:/prompts/system-router.st")
  private Resource systemRouterResource;

  public RouterInterceptor(Map<String, ChatModel> chatModels, CoreProperties properties) {
    this.chatModels = Map.copyOf(Objects.requireNonNullElse(chatModels, Map.of()));
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    if (!isRouterEnabled()) return context;

    String provider = resolveProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      logger.warn("Router provider '{}' is not registered.", provider);
      return context;
    }

    Prompt prompt = buildRouterPrompt(provider, context);
    return executeRouting(model, prompt, context);
  }

  private boolean isRouterEnabled() {
    return Optional.ofNullable(properties.orchestration())
        .map(CoreProperties.OrchestrationConfig::router)
        .map(CoreProperties.InterceptorConfig::enabled)
        .orElse(false);
  }

  private String resolveProvider() {
    return Optional.ofNullable(properties.orchestration())
        .map(CoreProperties.OrchestrationConfig::router)
        .map(CoreProperties.InterceptorConfig::provider)
        .orElse(properties.defaultProvider());
  }

  private Prompt buildRouterPrompt(String provider, PromptContext context) {
    var sysMsg = new SystemPromptTemplate(systemRouterResource).createMessage();
    var userMsg = new UserMessage(context.refinedPrompt());
    var opt = properties.orchestration().router();
    ChatOptions options = PipelineOptionsBuilder.build(provider, opt.model(), opt.temperature());
    return new Prompt(List.of(sysMsg, userMsg), options);
  }

  private PromptContext executeRouting(ChatModel model, Prompt prompt, PromptContext context) {
    try {
      return PipelineInterceptors.extractResponseText(model.call(prompt))
          .map(String::toLowerCase)
          .filter(chatModels::containsKey)
          .map(context::withRoutedProvider)
          .orElse(context);
    } catch (Exception e) {
      logger.error("Failed to dynamically route prompt.", e);
      return context;
    }
  }

  @Override
  public int getOrder() {
    return 4;
  }
}

class PipelineOptionsBuilder {
  static ChatOptions build(String provider, String modelName, Double temp) {
    return switch (provider.toLowerCase()) {
      case "ollama" -> OllamaChatOptions.builder().model(modelName).temperature(temp).build();
      case "openai" -> OpenAiChatOptions.builder().model(modelName).temperature(temp).build();
      default -> null;
    };
  }
}

@Component
class OrasakaMemoryInterceptor implements OrasakaContextInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(OrasakaMemoryInterceptor.class);
  private final OrasakaMemoryResolver memoryResolver;

  public OrasakaMemoryInterceptor(OrasakaMemoryResolver memoryResolver) {
    this.memoryResolver =
        Objects.requireNonNull(memoryResolver, "OrasakaMemoryResolver must not be null");
  }

  @Override
  public ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    resolveConversationId(request)
        .ifPresent(
            convId -> {
              List<Message> history = memoryResolver.resolve(convId).get(convId);
              if (!history.isEmpty()) messages.addAll(0, history);
            });
    return options;
  }

  @Override
  public void postProcess(InternalChatRequest request, String promptText, String responseText) {
    resolveConversationId(request)
        .ifPresent(
            convId -> {
              ChatMemory chatMemory = memoryResolver.resolve(convId);
              List<Message> newMessages = new ArrayList<>();
              if (promptText != null && !promptText.isBlank())
                newMessages.add(new UserMessage(promptText));
              newMessages.add(new AssistantMessage(responseText));
              chatMemory.add(convId, newMessages);
            });
  }

  private Optional<String> resolveConversationId(InternalChatRequest request) {
    return Optional.ofNullable(request.context())
        .map(ctx -> ctx.conversationId())
        .filter(id -> !id.isBlank());
  }
}

@Component
class OrasakaMcpInterceptor implements OrasakaContextInterceptor {
  private final McpOrchestrator mcpOrchestrator;

  public OrasakaMcpInterceptor(McpOrchestrator mcpOrchestrator) {
    this.mcpOrchestrator =
        Objects.requireNonNull(mcpOrchestrator, "McpOrchestrator must not be null");
  }

  @Override
  public ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    Optional.ofNullable(mcpOrchestrator.resolveExternalContext())
        .filter(ctx -> !ctx.isBlank())
        .ifPresent(ctx -> messages.add(new SystemMessage("MCP Context: " + ctx)));
    return options;
  }
}

@Component
class OrasakaRagInterceptor implements OrasakaContextInterceptor {
  private final CoreProperties properties;
  private final OrasakaKnowledgeService knowledgeService;

  public OrasakaRagInterceptor(
      CoreProperties properties, OrasakaKnowledgeService knowledgeService) {
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
    this.knowledgeService =
        Objects.requireNonNull(knowledgeService, "OrasakaKnowledgeService must not be null");
  }

  @Override
  public ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    Optional.ofNullable(properties.rag())
        .filter(CoreProperties.RagConfig::enabled)
        .ifPresent(
            rag -> {
              String context = knowledgeService.retrieveContext(promptText, rag.topK());
              if (context != null && !context.isBlank())
                messages.add(new SystemMessage("RAG Context: \n" + context));
            });
    return options;
  }
}

@Component
class OrasakaToolInterceptor implements OrasakaContextInterceptor {
  private final OrasakaToolRegistry toolRegistry;

  public OrasakaToolInterceptor(OrasakaToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  @Override
  public ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    if (toolRegistry == null || toolRegistry.getRegisteredTools().isEmpty()) return options;

    List<ToolCallback> demandedTools = collectDemandedTools(request, promptText);
    if (demandedTools.isEmpty()) return options;

    return attachTools(options, demandedTools);
  }

  private List<ToolCallback> collectDemandedTools(InternalChatRequest request, String promptText) {
    String lowerContext = extractFullText(request, promptText).toLowerCase();
    List<ToolCallback> demanded = new ArrayList<>();

    for (ToolCallback tool : toolRegistry.getRegisteredTools()) {
      String name = tool.getToolDefinition().name();
      if ("analyzePoster".equals(name) && demandsPosterTool(lowerContext)) demanded.add(tool);
      else if ("analyzeAudioExtract".equals(name) && demandsAudioTool(lowerContext))
        demanded.add(tool);
      else if (!"analyzePoster".equals(name) && !"analyzeAudioExtract".equals(name))
        demanded.add(tool);
    }
    return demanded;
  }

  private String extractFullText(InternalChatRequest request, String promptText) {
    StringBuilder text = new StringBuilder(promptText != null ? promptText : "");
    text.append(" ").append(request.prompt() != null ? request.prompt() : "");
    if (request.messages() != null) {
      request.messages().stream()
          .filter(m -> m.content() != null)
          .forEach(m -> text.append(" ").append(m.content()));
    }
    return text.toString();
  }

  private boolean demandsPosterTool(String lowerContext) {
    return lowerContext.contains("poster")
        || lowerContext.contains("image")
        || lowerContext.contains("visual")
        || lowerContext.contains("picture");
  }

  private boolean demandsAudioTool(String lowerContext) {
    return lowerContext.contains("audio")
        || lowerContext.contains("clip")
        || lowerContext.contains("voice")
        || lowerContext.contains("music")
        || lowerContext.contains("sound");
  }

  private ChatOptions attachTools(ChatOptions options, List<ToolCallback> demandedTools) {
    Set<String> toolNames =
        demandedTools.stream().map(t -> t.getToolDefinition().name()).collect(Collectors.toSet());
    return switch (options) {
      case OllamaChatOptions ollama ->
          OllamaChatOptions.builder()
              .model(ollama.getModel())
              .temperature(ollama.getTemperature())
              .numPredict(ollama.getNumPredict())
              .toolNames(toolNames)
              .toolCallbacks(demandedTools)
              .build();
      case OpenAiChatOptions openai ->
          OpenAiChatOptions.builder()
              .model(openai.getModel())
              .temperature(openai.getTemperature())
              .maxTokens(openai.getMaxTokens())
              .toolNames(toolNames)
              .toolCallbacks(demandedTools)
              .build();
      default -> options;
    };
  }
}
