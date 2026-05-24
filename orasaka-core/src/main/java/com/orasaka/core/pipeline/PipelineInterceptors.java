package com.orasaka.core.pipeline;

import com.orasaka.core.engine.CoreProperties;
import com.orasaka.core.support.OrasakaChatRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

public final class PipelineInterceptors {

  private PipelineInterceptors() {}
}

@Component
class UserContextResolver implements PromptInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(UserContextResolver.class);

  @Override
  public PromptContext intercept(PromptContext context) {
    logger.debug("Resolving user security context details...");
    var securityData = SecurityContextUtil.extractSecurityMetadata();
    if (!securityData.isEmpty()) {
      var newUserMetadata = new java.util.HashMap<>(context.userMetadata());
      newUserMetadata.putAll(securityData);
      logger.debug(
          "Successfully enriched user metadata with security claims: {}", securityData.keySet());
      return context.withUserMetadata(newUserMetadata);
    }
    return context;
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
    this.providers = providers != null ? providers : List.of();
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    logger.debug("Injecting system context signals. Provider count: {}", providers.size());
    Map<String, Object> newSystemMetadata = new java.util.HashMap<>(context.systemMetadata());
    for (SystemContextProvider provider : providers) {
      try {
        Map<String, Object> data = provider.getSystemContext();
        if (data != null) {
          newSystemMetadata.putAll(data);
        }
      } catch (Exception e) {
        logger.error("Error invoking SystemContextProvider: {}", provider.getClass().getName(), e);
      }
    }
    return context.withSystemMetadata(newSystemMetadata);
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

  public RefinerInterceptor(Map<String, ChatModel> chatModels, CoreProperties properties) {
    this.chatModels = chatModels;
    this.properties = properties;
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    var opt = properties.orchestration() != null ? properties.orchestration().refiner() : null;
    if (opt == null || !opt.enabled()) {
      logger.debug("Refiner interceptor is disabled. Skipping prompt refinement.");
      return context;
    }

    String provider = opt.provider() != null ? opt.provider() : properties.defaultProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      logger.warn(
          "Refinement provider '{}' is not registered in chat models. Skipping refinement.",
          provider);
      return context;
    }

    logger.info("Refining prompt via LLM provider: {}, model: {}", provider, opt.model());

    String systemInstruction =
        "You are a prompt refinement engine for the Orasaka System. "
            + "Your goal is to enrich the user's raw query by incorporating the provided user and system metadata, "
            + "generating a highly detailed, precise, and contextual instruction prompt for a downstream LLM. "
            + "Do NOT answer the query yourself. Output ONLY the refined prompt, with no intro, markdown formatting, or wrap-up text.";

    String contextMatrix =
        String.format(
            "=== USER METADATA ===\n%s\n\n=== SYSTEM METADATA ===\n%s\n\n=== RAW QUERY ===\n%s",
            context.userMetadata().toString(),
            context.systemMetadata().toString(),
            context.rawUserQuery());

    ChatOptions options = buildOptions(provider, opt.model(), opt.temperature());
    Prompt prompt =
        new Prompt(
            List.of(new SystemMessage(systemInstruction), new UserMessage(contextMatrix)), options);

    try {
      ChatResponse response = model.call(prompt);
      if (response != null
          && response.getResult() != null
          && response.getResult().getOutput() != null) {
        String refined = response.getResult().getOutput().getText().strip();
        if (!refined.isBlank()) {
          logger.debug("Refinement generated: {}", refined);
          return context.withRefinedPrompt(refined);
        }
      }
    } catch (Exception e) {
      logger.error("Failed to refine prompt using model, falling back to raw query.", e);
    }
    return context;
  }

  private ChatOptions buildOptions(String provider, String modelName, Double temp) {
    return switch (provider.toLowerCase()) {
      case "ollama" -> OllamaChatOptions.builder().model(modelName).temperature(temp).build();
      case "openai" -> OpenAiChatOptions.builder().model(modelName).temperature(temp).build();
      default -> null;
    };
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

  public RouterInterceptor(Map<String, ChatModel> chatModels, CoreProperties properties) {
    this.chatModels = chatModels;
    this.properties = properties;
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    var opt = properties.orchestration() != null ? properties.orchestration().router() : null;
    if (opt == null || !opt.enabled()) {
      logger.debug("Router interceptor is disabled. Using default provider.");
      return context;
    }

    String provider = opt.provider() != null ? opt.provider() : properties.defaultProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      logger.warn(
          "Router provider '{}' is not registered in chat models. Skipping routing.", provider);
      return context;
    }

    logger.info("Routing prompt via LLM provider: {}, model: {}", provider, opt.model());

    String systemInstruction =
        "You are a prompt router for the Orasaka System. "
            + "Evaluate the user's refined query and select the most specialized LLM execution provider. "
            + "Available providers are: 'ollama' (for generic, local, lightweight operations) "
            + "and 'openai' (for high-fidelity, complex logic, code generation, or translation). "
            + "Output exactly one word representing the provider key: either 'ollama' or 'openai'. "
            + "Do NOT write any additional text, explanation, or markdown wrappers.";

    ChatOptions options = buildOptions(provider, opt.model(), opt.temperature());
    Prompt prompt =
        new Prompt(
            List.of(new SystemMessage(systemInstruction), new UserMessage(context.refinedPrompt())),
            options);

    try {
      ChatResponse response = model.call(prompt);
      if (response != null
          && response.getResult() != null
          && response.getResult().getOutput() != null) {
        String routed = response.getResult().getOutput().getText().strip().toLowerCase();
        if (chatModels.containsKey(routed)) {
          logger.info("Pipeline routed request to target provider: '{}'", routed);
          return context.withRoutedProvider(routed);
        } else {
          logger.warn(
              "Pipeline routed request to unregistered provider key: '{}'. Ignoring selection.",
              routed);
        }
      }
    } catch (Exception e) {
      logger.error(
          "Failed to dynamically route prompt using model, falling back to default provider.", e);
    }
    return context;
  }

  private ChatOptions buildOptions(String provider, String modelName, Double temp) {
    return switch (provider.toLowerCase()) {
      case "ollama" -> OllamaChatOptions.builder().model(modelName).temperature(temp).build();
      case "openai" -> OpenAiChatOptions.builder().model(modelName).temperature(temp).build();
      default -> null;
    };
  }

  @Override
  public int getOrder() {
    return 4;
  }
}

@Component
class OrasakaMemoryInterceptor implements OrasakaContextInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaMemoryInterceptor.class);

  private final OrasakaMemoryResolver memoryResolver;

  public OrasakaMemoryInterceptor(OrasakaMemoryResolver memoryResolver) {
    this.memoryResolver = memoryResolver;
  }

  @Override
  public ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    String conversationId = (request.context() != null) ? request.context().conversationId() : null;
    if (conversationId != null && !conversationId.isBlank() && memoryResolver != null) {
      ChatMemory chatMemory = memoryResolver.resolve(conversationId);
      List<Message> history = chatMemory.get(conversationId);
      if (history != null && !history.isEmpty()) {
        messages.addAll(0, history);
        logger.debug(
            "Loaded {} messages from ChatMemory for conversationId: {}",
            history.size(),
            conversationId);
      } else {
        logger.debug("No history found in ChatMemory for conversationId: {}", conversationId);
      }
    }
    return options;
  }

  @Override
  public void postProcess(OrasakaChatRequest request, String promptText, String responseText) {
    String conversationId = (request.context() != null) ? request.context().conversationId() : null;
    if (conversationId != null && !conversationId.isBlank() && memoryResolver != null) {
      ChatMemory chatMemory = memoryResolver.resolve(conversationId);
      List<Message> newMessages = new ArrayList<>();
      if (promptText != null && !promptText.isBlank()) {
        newMessages.add(new UserMessage(promptText));
      }
      newMessages.add(new AssistantMessage(responseText));
      chatMemory.add(conversationId, newMessages);
      logger.debug("Saved new messages to ChatMemory for conversationId: {}", conversationId);
    }
  }
}

@Component
class OrasakaMcpInterceptor implements OrasakaContextInterceptor {

  private final McpOrchestrator mcpOrchestrator;

  public OrasakaMcpInterceptor(McpOrchestrator mcpOrchestrator) {
    this.mcpOrchestrator = mcpOrchestrator;
  }

  @Override
  public ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    if (mcpOrchestrator != null) {
      String mcpContext = mcpOrchestrator.resolveExternalContext();
      if (mcpContext != null && !mcpContext.isBlank()) {
        messages.add(new SystemMessage("MCP Context: " + mcpContext));
      }
    }
    return options;
  }
}

@Component
class OrasakaRagInterceptor implements OrasakaContextInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaRagInterceptor.class);

  private final CoreProperties properties;
  private final OrasakaKnowledgeService knowledgeService;

  public OrasakaRagInterceptor(
      CoreProperties properties, OrasakaKnowledgeService knowledgeService) {
    this.properties = properties;
    this.knowledgeService = knowledgeService;
  }

  @Override
  public ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    int ragContextSize = 0;
    if (properties.rag() != null && properties.rag().enabled() && knowledgeService != null) {
      String context = knowledgeService.retrieveContext(promptText, properties.rag().topK());
      if (context != null && !context.isBlank()) {
        messages.add(new SystemMessage("RAG Context: \n" + context));
        ragContextSize = context.length();
      }
    }
    logger.debug("RAG Injection Context Size: {} characters", ragContextSize);
    return options;
  }
}

@Component
class OrasakaToolInterceptor implements OrasakaContextInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaToolInterceptor.class);

  private final OrasakaToolRegistry toolRegistry;

  public OrasakaToolInterceptor(OrasakaToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  @Override
  public ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    if (toolRegistry == null || toolRegistry.getRegisteredTools().isEmpty()) {
      return options;
    }

    List<ToolCallback> demandedTools = new java.util.ArrayList<>();
    for (ToolCallback tool : toolRegistry.getRegisteredTools()) {
      String name = tool.getToolDefinition().name();
      boolean demand = true;
      if ("analyzePoster".equals(name)) {
        demand = demandsPosterTool(request, promptText);
      } else if ("analyzeAudioExtract".equals(name)) {
        demand = demandsAudioTool(request, promptText);
      }
      if (demand) {
        demandedTools.add(tool);
      }
    }

    if (demandedTools.isEmpty()) {
      return options;
    }

    options = attachTools(options, demandedTools);
    logger.debug("Attached {} tools to ChatOptions", demandedTools.size());
    return options;
  }

  private boolean demandsPosterTool(OrasakaChatRequest request, String promptText) {
    String text = (promptText != null ? promptText : "") + " " + request.prompt();
    if (request.messages() != null) {
      for (var msg : request.messages()) {
        if (msg.content() != null) {
          text += " " + msg.content();
        }
      }
    }
    String lower = text.toLowerCase();
    return lower.contains("poster")
        || lower.contains("image")
        || lower.contains("visual")
        || lower.contains("picture");
  }

  private boolean demandsAudioTool(OrasakaChatRequest request, String promptText) {
    String text = (promptText != null ? promptText : "") + " " + request.prompt();
    if (request.messages() != null) {
      for (var msg : request.messages()) {
        if (msg.content() != null) {
          text += " " + msg.content();
        }
      }
    }
    String lower = text.toLowerCase();
    return lower.contains("audio")
        || lower.contains("clip")
        || lower.contains("compliance")
        || lower.contains("sound")
        || lower.contains("voice")
        || lower.contains("music");
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
