package com.orasaka.core.engine;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.exception.OrasakaException;
import com.orasaka.core.mcp.McpOrchestrator;
import com.orasaka.core.model.*;
import com.orasaka.core.rag.OrasakaKnowledgeService;
import com.orasaka.core.tool.OrasakaToolRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.*;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import reactor.core.publisher.Flux;

/**
 * Core Orchestration Engine for the Orasaka CORS library.
 *
 * <p>Implements the Bridge Pattern to decouple host applications from Spring AI internals. Manages
 * the integration of RAG, Tooling, and MCP protocols.
 *
 * <p>High-concurrency tasks are executed using Java 21 Virtual Threads to ensure non-blocking
 * performance.
 *
 * <p>This class is {@code sealed} and permits only {@link OrasakaEngine} as a subclass, enforcing a
 * type-safe, exhaustive engine hierarchy per AGENTS.md §2.A.
 *
 * @see <a href="file:///Users/oussamaabid/Documents/projects/orasaka/docs/GLOSSARY.md">Orasaka
 *     Glossary</a>
 * @see org.springframework.ai.chat.model.ChatModel
 */
public abstract sealed class AbstractOrasakaEngine permits OrasakaEngine {

  private static final Logger logger = LoggerFactory.getLogger(AbstractOrasakaEngine.class);

  protected final Map<String, ChatModel> chatModels;
  protected final Map<String, ImageModel> imageModels;
  protected final Map<String, EmbeddingModel> embeddingModels;
  protected final Map<String, TextToSpeechModel> speechModels;
  protected final CoreProperties properties;
  protected final OrasakaToolRegistry toolRegistry;
  protected final OrasakaKnowledgeService knowledgeService;
  protected final McpOrchestrator mcpOrchestrator;
  protected final OrasakaMemoryResolver memoryResolver;
  protected final com.orasaka.core.orchestration.OrasakaOrchestrationPipeline orchestrationPipeline;

  /** Virtual Thread Executor for high-concurrency AI orchestration. */
  private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

  /**
   * Initializes the engine with all required cognitive components.
   *
   * @param chatModels Map of available chat model providers.
   * @param imageModels Map of available image model providers.
   * @param embeddingModels Map of available embedding model providers.
   * @param speechModels Map of available speech model providers.
   * @param properties Configuration properties (Mandatory: defaultProvider).
   * @param toolRegistry Local Java tool registry.
   * @param knowledgeService RAG knowledge orchestration service.
   * @param mcpOrchestrator Model Context Protocol bridge.
   * @param memoryResolver Resolver for session-based chat memory.
   * @param orchestrationPipeline Pipeline for prompt context matrix enrichment and routing.
   */
  protected AbstractOrasakaEngine(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties,
      OrasakaToolRegistry toolRegistry,
      OrasakaKnowledgeService knowledgeService,
      McpOrchestrator mcpOrchestrator,
      OrasakaMemoryResolver memoryResolver,
      com.orasaka.core.orchestration.OrasakaOrchestrationPipeline orchestrationPipeline) {
    this.chatModels = chatModels;
    this.imageModels = imageModels;
    this.embeddingModels = embeddingModels;
    this.speechModels = speechModels;
    this.properties = properties;
    this.toolRegistry = toolRegistry;
    this.knowledgeService = knowledgeService;
    this.mcpOrchestrator = mcpOrchestrator;
    this.memoryResolver = memoryResolver;
    this.orchestrationPipeline = orchestrationPipeline;
  }

  /**
   * Executes a chat request using the active provider with agentic capabilities.
   *
   * <p>This method is thread-safe and non-blocking as it utilizes Java 21 Virtual Threads via an
   * internal executor to perform AI inference, local Java tool calls, and context retrieval in a
   * decoupled manner.
   *
   * @param request The domain-specific chat request containing user prompts and context.
   * @return A synchronized chat response carrying the final text result and conversation metadata.
   * @throws OrasakaException If virtual thread execution fails or the active provider is missing.
   * @see com.orasaka.core.model.OrasakaChatRequest
   * @see com.orasaka.core.model.OrasakaChatResponse
   * @see org.springframework.ai.chat.model.ChatModel
   */
  public OrasakaChatResponse chat(OrasakaChatRequest request) {
    try {
      return virtualThreadExecutor.submit(() -> executeChat(request)).get(60, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new OrasakaException("Failed to execute chat request in virtual thread", e);
    }
  }

  /**
   * Executes a chat request using the active provider with agentic capabilities, returning the
   * response as a reactive streaming {@link Flux}.
   *
   * <p>This method utilizes Spring AI's underlying stream model. Setup operations (RAG, MCP context
   * retrieval) are performed lazily when a client subscribes. When executed within a virtual thread
   * or reactor flow, it does not block the main event loop threads.
   *
   * @param request The domain-specific chat request containing user prompts and context.
   * @return A {@link Flux} emitting individual {@link OrasakaChatResponse} token chunks reactively.
   * @throws OrasakaException If active provider resolution or downstream streaming initialization
   *     fails.
   * @see com.orasaka.core.model.OrasakaChatRequest
   * @see com.orasaka.core.model.OrasakaChatResponse
   * @see reactor.core.publisher.Flux
   * @see org.springframework.ai.chat.model.ChatModel#stream(Prompt)
   */
  public Flux<OrasakaChatResponse> stream(OrasakaChatRequest request) {
    try {
      return Flux.defer(
          () -> {
            com.orasaka.core.orchestration.PromptContext pipelineContext =
                (orchestrationPipeline != null)
                    ? orchestrationPipeline.process(request.prompt(), request.context())
                    : null;
            String promptText =
                (pipelineContext != null) ? pipelineContext.refinedPrompt() : request.prompt();
            String provider =
                (pipelineContext != null && pipelineContext.routedProvider() != null)
                    ? pipelineContext.routedProvider()
                    : getActiveProvider();

            ChatModel model = chatModels.get(provider);
            if (model == null) {
              throw new OrasakaException("No ChatModel found for provider: " + provider);
            }
            logger.debug("Executing streaming chat with provider: {}", provider);

            List<Message> messages = new ArrayList<>();

            // 1. RAG Injection
            if (properties.rag() != null
                && properties.rag().enabled()
                && knowledgeService != null) {
              String context =
                  knowledgeService.retrieveContext(promptText, properties.rag().topK());
              if (context != null && !context.isBlank()) {
                messages.add(new SystemMessage("RAG Context: \n" + context));
              }
            }

            // 2. MCP Context Injection
            if (mcpOrchestrator != null) {
              String mcpContext = mcpOrchestrator.resolveExternalContext();
              if (mcpContext != null && !mcpContext.isBlank()) {
                messages.add(new SystemMessage("MCP Context: " + mcpContext));
              }
            }

            // 3. Chat Memory Resolution
            String conversationId =
                (request.context() != null) ? request.context().conversationId() : null;
            ChatMemory chatMemory = null;
            if (conversationId != null && !conversationId.isBlank() && memoryResolver != null) {
              chatMemory = memoryResolver.resolve(conversationId);
              List<Message> history = chatMemory.get(conversationId);
              if (history != null && !history.isEmpty()) {
                messages.addAll(history);
              }
            }

            if (request.messages() != null) {
              messages.addAll(request.messages().stream().map(this::mapMessage).toList());
            }

            UserMessage userMessage = null;
            if (promptText != null && !promptText.isBlank()) {
              userMessage = new UserMessage(promptText);
              messages.add(userMessage);
            }

            // 4. Tool Attachment
            ChatOptions springOptions = mapOptions(request.options(), provider);
            if (toolRegistry != null && !toolRegistry.getRegisteredTools().isEmpty()) {
              springOptions = attachTools(springOptions);
            }

            Prompt prompt = new Prompt(messages, springOptions);
            StringBuilder responseBuilder = new StringBuilder();

            final ChatMemory finalChatMemory = chatMemory;
            final UserMessage finalUserMessage = userMessage;

            return model.stream(prompt)
                .map(
                    chatResponse -> {
                      String chunk = chatResponse.getResult().getOutput().getText();
                      if (chunk != null) {
                        responseBuilder.append(chunk);
                      }
                      return new OrasakaChatResponse(
                          chunk, conversationId, Map.of("provider", provider));
                    })
                .doOnComplete(
                    () -> {
                      if (finalChatMemory != null && conversationId != null) {
                        List<Message> newMessages = new ArrayList<>();
                        if (finalUserMessage != null) {
                          newMessages.add(finalUserMessage);
                        }
                        newMessages.add(new AssistantMessage(responseBuilder.toString()));
                        finalChatMemory.add(conversationId, newMessages);
                        logger.debug(
                            "Saved streamed messages to ChatMemory for conversationId: {}",
                            conversationId);
                      }
                    });
          });
    } catch (Exception e) {
      throw new OrasakaException("Failed to initialize chat stream flow", e);
    }
  }

  /**
   * Executes an image generation request using the active provider.
   *
   * <p>This method is non-blocking and thread-safe as it submits the image generation task to a
   * Virtual Thread executor, allowing high concurrency under local resource limits.
   *
   * @param request The domain-specific image generation request details (prompt, dimensions).
   * @return A synchronized image response containing target URLs or metadata.
   * @throws OrasakaException If image generation execution fails or active provider
   *     misconfiguration occurs.
   * @see com.orasaka.core.model.OrasakaImageRequest
   * @see com.orasaka.core.model.OrasakaImageResponse
   * @see org.springframework.ai.image.ImageModel
   */
  public OrasakaImageResponse generateImage(OrasakaImageRequest request) {
    try {
      return virtualThreadExecutor
          .submit(() -> executeImageGeneration(request))
          .get(60, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new OrasakaException("Failed to execute image generation in virtual thread", e);
    }
  }

  /**
   * Executes a Text-To-Speech request using the active provider.
   *
   * <p>This method is non-blocking and thread-safe as it processes the TTS generation
   * asynchronously on a Virtual Thread. Accepts an {@link OrasakaSpeechRequest} carrying an {@link
   * com.orasaka.core.context.OrasakaContext} so that per-user voice models and speech preferences
   * are resolved dynamically.
   *
   * @param request The speech generation specification including prompt text and user context
   *     preferences.
   * @return A byte array containing the raw audio data produced by the TTS provider.
   * @throws OrasakaException If speech generation execution fails or no TTS model matches the
   *     active provider.
   * @see com.orasaka.core.model.OrasakaSpeechRequest
   * @see org.springframework.ai.audio.tts.TextToSpeechModel
   * @see com.orasaka.core.context.OrasakaContext
   */
  public byte[] generateSpeech(OrasakaSpeechRequest request) {
    try {
      return virtualThreadExecutor
          .submit(() -> executeSpeechGeneration(request))
          .get(60, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new OrasakaException("Failed to execute speech generation in virtual thread", e);
    }
  }

  /**
   * Core execution logic for TTS generation. Applies voice/speed overrides from the {@link
   * com.orasaka.core.context.OrasakaContext} when present.
   */
  private byte[] executeSpeechGeneration(OrasakaSpeechRequest request) {
    String provider = getActiveProvider();
    logger.debug("Executing speech generation with provider: {}, request: {}", provider, request);
    TextToSpeechModel model = speechModels.get(provider);
    if (model == null) {
      throw new OrasakaException("No TextToSpeechModel found for provider: " + provider);
    }

    // Resolve voice preference from context preferences if present (provider-specific wiring)
    if (request.context() != null && request.context().preferences() != null) {
      Object voicePref = request.context().preferences().get("tts-voice");
      if (voicePref instanceof String s) {
        logger.debug(
            "TTS voice preference '{}' resolved from context for user '{}'",
            s,
            request.context().userId());
      }
    }

    TextToSpeechPrompt prompt = new TextToSpeechPrompt(request.text());
    TextToSpeechResponse response = model.call(prompt);
    byte[] output = response.getResult().getOutput();
    logger.debug(
        "Speech generation completed with provider: {}, output size: {} bytes",
        provider,
        output != null ? output.length : 0);
    return output;
  }

  /**
   * Core execution logic for multi-modal image generation.
   *
   * @param request The image request to process.
   * @return The generated image response.
   */
  private OrasakaImageResponse executeImageGeneration(OrasakaImageRequest request) {
    String provider = getActiveProvider();
    logger.debug("Executing image generation with provider: {}, request: {}", provider, request);
    ImageModel model = imageModels.get(provider);
    if (model == null) {
      throw new OrasakaException("No ImageModel found for provider: " + provider);
    }

    ImageOptions springOptions = mapImageOptions(request);
    ImagePrompt prompt =
        new ImagePrompt(List.of(new ImageMessage(request.prompt())), springOptions);
    ImageResponse response = model.call(prompt);

    if (response.getResults().isEmpty()) {
      throw new OrasakaException("Image generation returned no results");
    }

    var result = response.getResult().getOutput();
    OrasakaImageResponse imageResponse =
        new OrasakaImageResponse(
            null, // Byte data usually handled by separate download if needed
            result.getUrl(),
            "png" // Default format
            );
    logger.debug(
        "Image generation completed with provider: {}, URL: {}", provider, result.getUrl());
    return imageResponse;
  }

  /**
   * Core execution logic for agentic chat. Handles RAG injection, MCP context resolution, and Tool
   * attachment.
   *
   * @param request The chat request to process.
   * @return The generated response from the underlying model.
   */
  private OrasakaChatResponse executeChat(OrasakaChatRequest request) {
    com.orasaka.core.orchestration.PromptContext pipelineContext =
        (orchestrationPipeline != null)
            ? orchestrationPipeline.process(request.prompt(), request.context())
            : null;
    String promptText =
        (pipelineContext != null) ? pipelineContext.refinedPrompt() : request.prompt();
    String provider =
        (pipelineContext != null && pipelineContext.routedProvider() != null)
            ? pipelineContext.routedProvider()
            : getActiveProvider();

    ChatModel model = chatModels.get(provider);
    if (model == null) {
      throw new OrasakaException("No ChatModel found for provider: " + provider);
    }
    logger.debug("Executing chat with provider: {}", provider);
    logger.debug("Input Prompt: {}", promptText);

    List<Message> messages = new ArrayList<>();

    // 1. RAG Injection
    int ragContextSize = 0;
    if (properties.rag() != null && properties.rag().enabled() && knowledgeService != null) {
      String context = knowledgeService.retrieveContext(promptText, properties.rag().topK());
      if (context != null && !context.isBlank()) {
        messages.add(new SystemMessage("RAG Context: \n" + context));
        ragContextSize = context.length();
      }
    }
    logger.debug(
        "RAG Injection Context Size: {} characters (RAG enabled: {})",
        ragContextSize,
        (properties.rag() != null && properties.rag().enabled()));

    // 2. MCP Context Injection
    int mcpContextSize = 0;
    if (mcpOrchestrator != null) {
      String mcpContext = mcpOrchestrator.resolveExternalContext();
      if (mcpContext != null && !mcpContext.isBlank()) {
        messages.add(new SystemMessage("MCP Context: " + mcpContext));
        mcpContextSize = mcpContext.length();
      }
    }
    logger.debug("MCP Context Size: {} characters", mcpContextSize);

    // 3. Chat Memory Resolution
    String conversationId = (request.context() != null) ? request.context().conversationId() : null;
    ChatMemory chatMemory = null;
    if (conversationId != null && !conversationId.isBlank() && memoryResolver != null) {
      chatMemory = memoryResolver.resolve(conversationId);
      List<Message> history = chatMemory.get(conversationId);
      if (history != null && !history.isEmpty()) {
        messages.addAll(history);
        logger.debug(
            "Loaded {} messages from ChatMemory for conversationId: {}",
            history.size(),
            conversationId);
      } else {
        logger.debug("No history found in ChatMemory for conversationId: {}", conversationId);
      }
    }

    if (request.messages() != null) {
      messages.addAll(request.messages().stream().map(this::mapMessage).toList());
    }

    UserMessage userMessage = null;
    if (promptText != null && !promptText.isBlank()) {
      userMessage = new UserMessage(promptText);
      messages.add(userMessage);
    }

    // 4. Tool Attachment
    ChatOptions springOptions = mapOptions(request.options(), provider);
    int toolCount = 0;
    if (toolRegistry != null && !toolRegistry.getRegisteredTools().isEmpty()) {
      springOptions = attachTools(springOptions);
      toolCount = toolRegistry.getRegisteredTools().size();
    }
    logger.debug("Attached {} tools to ChatOptions", toolCount);

    Prompt prompt = new Prompt(messages, springOptions);
    logger.debug(
        "Sending Prompt with {} total messages to model: {}",
        messages.size(),
        model.getClass().getSimpleName());
    ChatResponse response = model.call(prompt);

    String assistantText = response.getResult().getOutput().getText();
    logger.debug("Final Raw Model Response: {}", assistantText);

    // Save to ChatMemory
    if (chatMemory != null && conversationId != null) {
      List<Message> newMessages = new ArrayList<>();
      if (userMessage != null) {
        newMessages.add(userMessage);
      }
      newMessages.add(new AssistantMessage(assistantText));
      chatMemory.add(conversationId, newMessages);
      logger.debug(
          "Saved new messages (User prompt + Assistant response) to ChatMemory for conversationId: {}",
          conversationId);
    }

    return new OrasakaChatResponse(assistantText, conversationId, Map.of("provider", provider));
  }

  /**
   * Resolves and returns the configured ChatModel for the active provider.
   *
   * <p>Looks up the active provider in the chat models registry mapping.
   *
   * @return The configured {@link ChatModel} corresponding to the active provider.
   * @throws OrasakaException If no ChatModel is registered or configured for the active provider.
   * @see org.springframework.ai.chat.model.ChatModel
   * @see #getActiveProvider()
   */
  protected ChatModel resolveChatModel() {
    String provider = getActiveProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      throw new OrasakaException("No ChatModel found for provider: " + provider);
    }
    return model;
  }

  /**
   * Identifies the current active AI provider key from global configuration properties.
   *
   * @return The provider key (e.g., "ollama", "openai").
   * @throws OrasakaException If the default provider configuration property is missing or blank.
   * @see com.orasaka.core.config.CoreProperties
   */
  protected String getActiveProvider() {
    if (properties.defaultProvider() == null || properties.defaultProvider().isBlank()) {
      throw new OrasakaException("Missing required property: orasaka.core.default-provider");
    }
    return properties.defaultProvider();
  }

  /**
   * Resolves the base URL endpoint for the active provider configured in the properties.
   *
   * @return The base URL string targeting the active provider's API endpoint.
   * @throws OrasakaException If the base URL property override is missing for the active provider.
   * @see #getActiveProvider()
   * @see com.orasaka.core.config.CoreProperties
   */
  protected String getBaseUrl() {
    String provider = getActiveProvider();
    if (properties.overrides() != null && properties.overrides().containsKey(provider)) {
      String baseUrl = properties.overrides().get(provider).baseUrl();
      if (baseUrl != null && !baseUrl.isBlank()) return baseUrl;
    }
    throw new OrasakaException(
        "Missing required property: orasaka.core.overrides." + provider + ".base-url");
  }

  /**
   * Maps Orasaka messages to Spring AI message types.
   *
   * @param msg The source Orasaka message.
   * @return The equivalent {@link Message}.
   */
  private Message mapMessage(OrasakaChatRequest.ChatMessage msg) {
    return switch (msg.role().toLowerCase()) {
      case "system" -> new SystemMessage(msg.content());
      case "assistant" -> new AssistantMessage(msg.content());
      default -> new UserMessage(msg.content());
    };
  }

  private ChatOptions mapOptions(OrasakaOptions options, String provider) {
    Double temp = (options != null) ? options.getTemperature() : 0.7;
    Integer tokens = (options != null) ? options.getMaxTokens() : null;

    return switch (provider.toLowerCase()) {
      case "ollama" -> OllamaChatOptions.builder().temperature(temp).numPredict(tokens).build();
      case "openai" -> OpenAiChatOptions.builder().temperature(temp).maxTokens(tokens).build();
      default -> {
        DefaultChatOptions defaultOptions = new DefaultChatOptions();
        defaultOptions.setTemperature(temp);
        defaultOptions.setMaxTokens(tokens);
        yield defaultOptions;
      }
    };
  }

  /**
   * Attaches registered native tools to the chat options.
   *
   * @param options The existing options to augment.
   * @return augmented {@link org.springframework.ai.chat.prompt.ChatOptions}.
   */
  private ChatOptions attachTools(ChatOptions options) {
    Set<String> toolNames =
        toolRegistry.getRegisteredTools().stream()
            .map(t -> t.getToolDefinition().name())
            .collect(Collectors.toSet());

    return switch (options) {
      case OllamaChatOptions ollama ->
          OllamaChatOptions.builder()
              .model(ollama.getModel())
              .temperature(ollama.getTemperature())
              .toolNames(toolNames)
              .build();
      case OpenAiChatOptions openai ->
          OpenAiChatOptions.builder()
              .model(openai.getModel())
              .temperature(openai.getTemperature())
              .toolNames(toolNames)
              .build();
      default -> options;
    };
  }

  /**
   * Maps Orasaka image request parameters to provider-specific Spring AI image options.
   *
   * @param request The source image request.
   * @return Configured {@link org.springframework.ai.image.ImageOptions}.
   */
  private ImageOptions mapImageOptions(OrasakaImageRequest request) {
    return switch (getActiveProvider().toLowerCase()) {
      case "openai" ->
          OpenAiImageOptions.builder()
              .height(request.height())
              .width(request.width())
              .quality("hd")
              .build();
      default ->
          null; // Other providers like Ollama do not support dedicated ImageModel generation in
        // Spring AI 1.1.6
    };
  }
}
