package com.orasaka.core.engine;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.event.OrasakaChatCompletedEvent;
import com.orasaka.core.exception.OrasakaException;
import com.orasaka.core.interceptors.OrasakaContextInterceptor;
import com.orasaka.core.model.*;
import com.orasaka.core.orchestration.OrasakaOrchestrationPipeline;
import com.orasaka.core.orchestration.PromptContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.*;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

/**
 * Core Orchestration Engine for the Orasaka CORS library.
 *
 * <p>Implements the Bridge Pattern to decouple host applications from Spring AI internals. Manages
 * the integration of RAG, Tooling, and MCP protocols via a generic Interceptor Chain.
 *
 * <p>This class is {@code sealed} and permits only {@link OrasakaEngine} as a subclass, enforcing a
 * type-safe, exhaustive engine hierarchy per AGENTS.md §2.A.
 */
public abstract sealed class AbstractOrasakaEngine permits OrasakaEngine {

  private static final Logger logger = LoggerFactory.getLogger(AbstractOrasakaEngine.class);

  protected final Map<String, ChatModel> chatModels;
  protected final Map<String, ImageModel> imageModels;
  protected final Map<String, EmbeddingModel> embeddingModels;
  protected final Map<String, TextToSpeechModel> speechModels;
  protected final CoreProperties properties;
  protected final List<OrasakaContextInterceptor> interceptors;
  protected final OrasakaOrchestrationPipeline orchestrationPipeline;
  protected final ApplicationEventPublisher eventPublisher;

  /**
   * Initializes the engine with all required cognitive components.
   *
   * @param chatModels Map of available chat model providers.
   * @param imageModels Map of available image model providers.
   * @param embeddingModels Map of available embedding model providers.
   * @param speechModels Map of available speech model providers.
   * @param properties Configuration properties (Mandatory: defaultProvider).
   * @param interceptors Cognitive interceptors pipeline.
   * @param orchestrationPipeline Pipeline for prompt context matrix enrichment and routing.
   * @param eventPublisher Spring application event publisher.
   */
  protected AbstractOrasakaEngine(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties,
      List<OrasakaContextInterceptor> interceptors,
      OrasakaOrchestrationPipeline orchestrationPipeline,
      ApplicationEventPublisher eventPublisher) {
    this.chatModels = chatModels;
    this.imageModels = imageModels;
    this.embeddingModels = embeddingModels;
    this.speechModels = speechModels;
    this.properties = properties;
    this.interceptors = interceptors != null ? List.copyOf(interceptors) : List.of();
    this.orchestrationPipeline = orchestrationPipeline;
    this.eventPublisher = eventPublisher;
  }

  /** Executes a chat request using the active provider with agentic capabilities. */
  public OrasakaChatResponse chat(OrasakaChatRequest request) {
    try {
      return executeChat(request);
    } catch (Exception e) {
      throw new OrasakaException("Failed to execute chat request", e);
    }
  }

  /** Executes a chat request reactively as a stream. */
  public Flux<OrasakaChatResponse> stream(OrasakaChatRequest request) {
    try {
      return Flux.defer(
          () -> {
            PromptContext pipelineContext =
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

            List<Message> messages =
                new ArrayList<>(request.compileMessages(promptText, this::mapMessage));
            ChatOptions springOptions =
                OrasakaOptionsMapper.mapOptions(request.options(), provider);

            // Execute pre-processing interceptors using Stream.reduce()
            ChatOptions finalOptions =
                interceptors.stream()
                    .reduce(
                        springOptions,
                        (opts, interceptor) ->
                            interceptor.preProcess(request, promptText, messages, opts),
                        (o1, o2) -> o1);

            Prompt prompt = new Prompt(messages, finalOptions);
            StringBuilder responseBuilder = new StringBuilder();
            String conversationId =
                (null != request.context()) ? request.context().conversationId() : null;

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
                      // Execute post-processing interceptors
                      interceptors.forEach(
                          interceptor ->
                              interceptor.postProcess(
                                  request, promptText, responseBuilder.toString()));

                      // Emit system event
                      OrasakaChatResponse completedResponse =
                          new OrasakaChatResponse(
                              responseBuilder.toString(),
                              conversationId,
                              Map.of("provider", provider));
                      eventPublisher.publishEvent(
                          new OrasakaChatCompletedEvent(request, completedResponse));
                    });
          });
    } catch (Exception e) {
      throw new OrasakaException("Failed to initialize chat stream flow", e);
    }
  }

  /** Executes an image generation request using the active provider. */
  public OrasakaImageResponse generateImage(OrasakaImageRequest request) {
    try {
      return executeImageGeneration(request);
    } catch (Exception e) {
      throw new OrasakaException("Failed to execute image generation", e);
    }
  }

  /** Executes a Text-To-Speech request using the active provider. */
  public byte[] generateSpeech(OrasakaSpeechRequest request) {
    try {
      return executeSpeechGeneration(request);
    } catch (Exception e) {
      throw new OrasakaException("Failed to execute speech generation", e);
    }
  }

  /** Core execution logic for TTS generation. */
  private byte[] executeSpeechGeneration(OrasakaSpeechRequest request) {
    String provider = getActiveProvider();
    logger.debug("Executing speech generation with provider: {}, request: {}", provider, request);
    TextToSpeechModel model = speechModels.get(provider);
    if (model == null) {
      throw new OrasakaException("No TextToSpeechModel found for provider: " + provider);
    }

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

  /** Core execution logic for multi-modal image generation. */
  private OrasakaImageResponse executeImageGeneration(OrasakaImageRequest request) {
    String provider = getActiveProvider();
    logger.debug("Executing image generation with provider: {}, request: {}", provider, request);
    ImageModel model = imageModels.get(provider);
    if (model == null) {
      throw new OrasakaException("No ImageModel found for provider: " + provider);
    }

    ImageOptions springOptions = OrasakaOptionsMapper.mapImageOptions(request, provider);
    ImagePrompt prompt =
        new ImagePrompt(List.of(new ImageMessage(request.prompt())), springOptions);
    ImageResponse response = model.call(prompt);

    if (response.getResults().isEmpty()) {
      throw new OrasakaException("Image generation returned no results");
    }

    var result = response.getResult().getOutput();
    OrasakaImageResponse imageResponse = new OrasakaImageResponse(null, result.getUrl(), "png");
    logger.debug(
        "Image generation completed with provider: {}, URL: {}", provider, result.getUrl());
    return imageResponse;
  }

  /** Core execution logic for agentic chat. */
  private OrasakaChatResponse executeChat(OrasakaChatRequest request) {
    PromptContext pipelineContext =
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

    List<Message> messages = new ArrayList<>(request.compileMessages(promptText, this::mapMessage));
    ChatOptions springOptions = OrasakaOptionsMapper.mapOptions(request.options(), provider);

    // Execute pre-processing interceptors using Stream.reduce()
    ChatOptions finalOptions =
        interceptors.stream()
            .reduce(
                springOptions,
                (opts, interceptor) -> interceptor.preProcess(request, promptText, messages, opts),
                (o1, o2) -> o1);

    Prompt prompt = new Prompt(messages, finalOptions);
    logger.debug(
        "Sending Prompt with {} total messages to model: {}",
        messages.size(),
        model.getClass().getSimpleName());
    ChatResponse response = model.call(prompt);

    String assistantText = response.getResult().getOutput().getText();
    logger.debug("Final Raw Model Response: {}", assistantText);

    // Execute post-processing interceptors
    interceptors.forEach(
        interceptor -> interceptor.postProcess(request, promptText, assistantText));

    String conversationId = (request.context() != null) ? request.context().conversationId() : null;
    OrasakaChatResponse chatResponse =
        new OrasakaChatResponse(assistantText, conversationId, Map.of("provider", provider));

    // Emit system event
    eventPublisher.publishEvent(new OrasakaChatCompletedEvent(request, chatResponse));

    return chatResponse;
  }

  protected ChatModel resolveChatModel() {
    String provider = getActiveProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      throw new OrasakaException("No ChatModel found for provider: " + provider);
    }
    return model;
  }

  protected String getActiveProvider() {
    if (properties.defaultProvider() == null || properties.defaultProvider().isBlank()) {
      throw new OrasakaException("Missing required property: orasaka.core.default-provider");
    }
    return properties.defaultProvider();
  }

  protected String getBaseUrl() {
    String provider = getActiveProvider();
    if (properties.overrides() != null && properties.overrides().containsKey(provider)) {
      String baseUrl = properties.overrides().get(provider).baseUrl();
      if (baseUrl != null && !baseUrl.isBlank()) return baseUrl;
    }
    throw new OrasakaException(
        "Missing required property: orasaka.core.overrides." + provider + ".base-url");
  }

  private Message mapMessage(OrasakaChatRequest.ChatMessage msg) {
    return switch (msg.role().toLowerCase()) {
      case "system" -> new SystemMessage(msg.content());
      case "assistant" -> new AssistantMessage(msg.content());
      default -> new UserMessage(msg.content());
    };
  }
}
