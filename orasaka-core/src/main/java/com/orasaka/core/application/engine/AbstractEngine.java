package com.orasaka.core.application.engine;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.application.pipeline.EnginePipelineBridge;
import com.orasaka.core.application.pipeline.EnginePipelineContext;
import com.orasaka.core.application.pipeline.EngineStreamBridge;
import com.orasaka.core.application.pipeline.OrchestrationPipeline;
import com.orasaka.core.application.pipeline.ProviderClassifier;
import com.orasaka.core.domain.event.ChatCompletedEvent;
import com.orasaka.core.domain.model.audio.AudioRequest;
import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.domain.model.chat.InternalChatResponse;
import com.orasaka.core.infrastructure.support.CoreException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

/**
 * Sealed abstract base class for the Orasaka AI orchestration engine.
 *
 * <p>This class defines the contract for all multi-modal AI operations — text chat, image
 * generation, speech synthesis, and token streaming. It is the <b>central inference backbone</b> of
 * {@code orasaka-core}, encapsulating:
 *
 * <ul>
 *   <li>Direct Spring AI model interfaces
 *   <li>Context-Matrix orchestration pipeline integration via {@link OrchestrationPipeline}
 *   <li>Pre/post-processing interceptor chain via {@link ContextInterceptor}
 *   <li>Domain event publishing for completed chat interactions
 * </ul>
 *
 * <p>The class is {@code sealed} to restrict the engine hierarchy to the single production subclass
 * {@link Engine}, enforcing exhaustive pattern-matching safety (ADR-003).
 *
 * <p><b>Concurrency</b>: All public methods are designed to execute inside Java 21 Virtual Threads
 * per the concurrency mandate in {@code AGENTS.md}. No {@code synchronized} blocks are used over
 * I/O loops.
 *
 * @see Engine
 * @since 1.0.0
 */
public abstract sealed class AbstractEngine permits Engine {

  private static final Logger logger = LoggerFactory.getLogger(AbstractEngine.class);

  /** Primary Spring AI model interface for text generation. */
  protected final ChatModel chatModel;

  /** Primary Spring AI model interface for text-to-speech generation. */
  protected final TextToSpeechModel ttsModel;

  /** Default provider name. */
  protected final String defaultProvider;

  /** Ordered list of context interceptors applied before and after inference. */
  final List<PromptContextInterceptor> interceptors;

  /** The orchestration pipeline (refiner, router, system/user context resolvers). */
  final OrchestrationPipeline pipeline;

  /** Spring application event publisher for domain events. */
  final ApplicationEventPublisher eventPublisher;

  /** Grouped infrastructure dependencies (pipeline bridge, stream bridge, credentials, etc.). */
  protected final EngineInfrastructure infra;

  /**
   * Constructs the engine with primary Spring AI models and pipeline components.
   *
   * @param chatModel Primary Spring AI chat model.
   * @param ttsModel Primary Spring AI text-to-speech model.
   * @param defaultProvider Default provider name.
   * @param interceptors Ordered list of context interceptors (nullable — defaults to empty).
   * @param pipeline The orchestration pipeline (nullable — bypassed if null).
   * @param publisher Spring event publisher for {@link ChatCompletedEvent}.
   * @param infra Grouped infrastructure dependencies.
   */
  protected AbstractEngine(
      ChatModel chatModel,
      TextToSpeechModel ttsModel,
      String defaultProvider,
      List<PromptContextInterceptor> interceptors,
      OrchestrationPipeline pipeline,
      ApplicationEventPublisher publisher,
      EngineInfrastructure infra) {
    this.chatModel = chatModel;
    this.ttsModel = ttsModel;
    this.defaultProvider = defaultProvider;
    this.interceptors = interceptors != null ? List.copyOf(interceptors) : List.of();
    this.pipeline = pipeline;
    this.eventPublisher = publisher;
    this.infra = infra;
  }

  /**
   * Executes a synchronous (non-streaming) chat inference.
   *
   * <p>Processing flow:
   *
   * <ol>
   *   <li>Compiles the full context via {@link EnginePipelineBridge#compileContext}
   *   <li>Invokes the resolved {@link ChatModel} with the assembled prompt
   *   <li>Falls back to tool-stripped inference on {@link IllegalStateException}
   *   <li>Runs all post-processing interceptors
   *   <li>Publishes an {@link ChatCompletedEvent}
   * </ol>
   *
   * @param request The internal chat request containing prompt, context, and options.
   * @return The engine's response with generated text, conversation ID, and metadata.
   * @throws CoreException If no suitable ChatModel is found for the resolved provider.
   */
  public InternalChatResponse chat(InternalChatRequest request) {
    var context =
        infra.pipelineBridge().compileContext(request, pipeline, defaultProvider, interceptors);
    logger.info("Engine executing chat model call with prompt: {}", context.toPrompt());
    ChatModel activeModel = resolveActiveModel(request, context);

    ChatResponse response;
    long startTime = System.nanoTime();
    try {
      response = activeModel.call(context.toPrompt());
      logger.info("Engine chat model call returned response: {}", response);
    } catch (RuntimeException e) {
      logger.warn(
          "Model '{}' failed tool execution activation, falling back to non-tool text inference.",
          context.provider(),
          e);
      response = activeModel.call(EnginePipelineBridge.removeTools(context.toPrompt()));
      logger.info("Engine fallback chat model call returned response: {}", response);
    } finally {
      closeContextResources(context);
    }
    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
    String text = extractResponseText(response);

    interceptors.forEach(i -> i.postProcess(request, context.promptText(), text));

    Map<String, Object> metadata = buildChatMetadata(context, response, durationMs);
    var out = new InternalChatResponse(text, context.conversationId(), Map.copyOf(metadata));
    eventPublisher.publishEvent(new ChatCompletedEvent(request, out));
    return out;
  }

  /** Resolves the active ChatModel, using commercial provider credentials when applicable. */
  private ChatModel resolveActiveModel(InternalChatRequest request, EnginePipelineContext context) {
    String resolvedProvider = context.provider();
    if (!ProviderClassifier.isCommercial(resolvedProvider)) {
      return this.chatModel;
    }
    String userId = request.context().userId();
    var apiKeyOpt = infra.credentialsProvider().getDecryptedApiKey(userId, resolvedProvider);
    if (apiKeyOpt.isEmpty()) {
      logger.warn(
          "No API key found for user {} and provider {}, falling back to local model.",
          userId,
          resolvedProvider);
      return this.chatModel;
    }
    String modelName = ProviderClassifier.resolveModelName(context.toPrompt().getOptions());
    return infra.modelFactory().createChatModel(resolvedProvider, modelName, apiKeyOpt.get());
  }

  /** Extracts text from a ChatResponse, returning empty string on null chain. */
  private String extractResponseText(ChatResponse response) {
    if (response == null
        || response.getResult() == null
        || response.getResult().getOutput() == null) {
      logger.warn(
          "Chat model response, result, or output was null. Falling back to default empty response.");
      return "";
    }
    return response.getResult().getOutput().getText();
  }

  /** Builds metadata map from context, response, and timing information. */
  private static Map<String, Object> buildChatMetadata(
      EnginePipelineContext context, ChatResponse response, long durationMs) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("provider", context.provider());
    metadata.put("durationMs", durationMs);
    if (response != null && response.getMetadata() != null) {
      var usage = response.getMetadata().getUsage();
      if (usage != null) {
        metadata.put("promptTokens", usage.getPromptTokens());
        metadata.put("generationTokens", usage.getCompletionTokens());
        metadata.put("totalTokens", usage.getTotalTokens());
      }
    }
    return metadata;
  }

  /** Closes any dynamic MCP client resources from the inference context. */
  private void closeContextResources(EnginePipelineContext context) {
    if (context.closeables() != null) {
      for (AutoCloseable c : context.closeables()) {
        try {
          c.close();
        } catch (Exception e) {
          logger.error("Failed to close dynamic MCP client", e);
        }
      }
    }
  }

  /**
   * Creates a reactive token stream for real-time chat inference.
   *
   * <p>Delegates stream assembly to {@link EngineStreamBridge#createStream}, which manages the
   * deferred Flux lifecycle, tool fallback, and post-completion event publishing.
   *
   * @param request The internal chat request containing prompt, context, and options.
   * @return A {@link Flux} emitting incremental {@link InternalChatResponse} chunks.
   */
  public Flux<InternalChatResponse> stream(InternalChatRequest request) {
    return infra
        .streamBridge()
        .createStream(request, pipeline, defaultProvider, interceptors, eventPublisher);
  }

  /**
   * Generates speech audio from text using the active TTS model.
   *
   * @param request The audio request containing the text and user context.
   * @return The raw audio bytes (MP3 format).
   * @throws CoreException If no TextToSpeechModel is registered for the active provider.
   */
  public byte[] generateSpeech(AudioRequest request) {
    OpenAiAudioSpeechOptions options =
        OpenAiAudioSpeechOptions.builder().model(request.model()).build();
    var response = ttsModel.call(new TextToSpeechPrompt(request.prompt(), options));
    return response.getResult().getOutput();
  }
}
