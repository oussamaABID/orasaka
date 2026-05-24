package com.orasaka.core.engine;

import com.orasaka.core.pipeline.ContextInterceptor;
import com.orasaka.core.pipeline.OrchestrationPipeline;
import com.orasaka.core.support.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageOptions;
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
 *   <li>Multi-provider model resolution via {@link EngineModelRegistry}
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
 * @see EngineModelRegistry
 * @see EnginePipelineBridge
 * @since 1.0.0
 */
public abstract sealed class AbstractEngine permits Engine {

  private static final Logger logger = LoggerFactory.getLogger(AbstractEngine.class);

  /** Centralized model resolution registry for all AI provider types. */
  final EngineModelRegistry registry;

  /** Ordered list of context interceptors applied before and after inference. */
  final List<ContextInterceptor> interceptors;

  /** The orchestration pipeline (refiner, router, system/user context resolvers). */
  final OrchestrationPipeline pipeline;

  /** Spring application event publisher for domain events. */
  final ApplicationEventPublisher eventPublisher;

  /**
   * Constructs the engine with provider model maps, configuration, and pipeline components.
   *
   * <p>Model maps are defensively wrapped in an immutable {@link EngineModelRegistry}. Interceptors
   * are defensively copied to an unmodifiable list.
   *
   * @param chat Map of provider name → {@link ChatModel} instances.
   * @param img Map of provider name → {@link ImageModel} instances.
   * @param emb Map of provider name → {@link EmbeddingModel} instances.
   * @param tts Map of provider name → {@link TextToSpeechModel} instances.
   * @param props Core configuration properties bound from {@code orasaka.core.*}.
   * @param interceptors Ordered list of context interceptors (nullable — defaults to empty).
   * @param pipeline The orchestration pipeline (nullable — bypassed if null).
   * @param publisher Spring event publisher for {@link ChatCompletedEvent}.
   */
  protected AbstractEngine(
      Map<String, ChatModel> chat,
      Map<String, ImageModel> img,
      Map<String, EmbeddingModel> emb,
      Map<String, TextToSpeechModel> tts,
      CoreProperties props,
      List<ContextInterceptor> interceptors,
      OrchestrationPipeline pipeline,
      ApplicationEventPublisher publisher) {
    this.registry = new EngineModelRegistry(chat, img, emb, tts, props);
    this.interceptors = interceptors != null ? List.copyOf(interceptors) : List.of();
    this.pipeline = pipeline;
    this.eventPublisher = publisher;
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
    var context = EnginePipelineBridge.compileContext(request, pipeline, registry, interceptors);
    ChatResponse response;
    try {
      response = registry.getChatModel(context.provider()).call(context.toPrompt());
    } catch (IllegalStateException e) {
      logger.warn(
          "Model '{}' failed tool execution activation, falling back to non-tool text inference.",
          context.provider(),
          e);
      response =
          registry
              .getChatModel(context.provider())
              .call(EnginePipelineBridge.removeTools(context.toPrompt()));
    }
    var text = response.getResult().getOutput().getText();

    interceptors.forEach(i -> i.postProcess(request, context.promptText(), text));
    var out =
        new InternalChatResponse(
            text, context.conversationId(), Map.of("provider", context.provider()));
    eventPublisher.publishEvent(new ChatCompletedEvent(request, out));
    return out;
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
    return EngineStreamBridge.createStream(
        request, pipeline, registry, interceptors, eventPublisher);
  }

  /**
   * Generates an image from a text prompt using the active image model.
   *
   * <p>The method resolves the active {@link ImageModel} from the registry, invokes it with Stable
   * Diffusion defaults (512×512, N=1), and extracts the raw image bytes from the response using
   * multiple fallback strategies (base64, reflection, URL).
   *
   * @param request The image generation request containing the text prompt.
   * @return The generated image response with raw bytes, Data URL, and format metadata.
   * @throws CoreException If no ImageModel is registered for the active provider.
   */
  public InternalImageResponse generateImage(InternalImageRequest request) {
    var model = registry.getActiveImageModel();
    OpenAiImageOptions executionOptions =
        OpenAiImageOptions.builder().model("stable-diffusion").N(1).height(512).width(512).build();
    var response = model.call(new ImagePrompt(request.prompt(), executionOptions));

    var generation = response.getResult();
    byte[] imageData = extractImageData(generation);
    String url = extractUrl(generation);

    if (imageData != null && url == null) {
      url = buildImageDataUrl(imageData);
    }
    return new InternalImageResponse(imageData, url, "png");
  }

  /**
   * Extracts raw image bytes from an {@link org.springframework.ai.image.ImageGeneration}. Attempts
   * base64 decoding first, then falls back to reflection-based extraction.
   *
   * @param generation The image generation result from the AI provider.
   * @return The raw image bytes, or {@code null} if extraction fails.
   */
  private byte[] extractImageData(org.springframework.ai.image.ImageGeneration generation) {
    if (generation == null || generation.getOutput() == null) return null;

    byte[] data = decodeBase64Image(generation.getOutput().getB64Json());
    if (data == null) {
      data = extractImageViaReflection(generation.getOutput());
    }
    return data;
  }

  /**
   * Extracts the URL from an image generation result.
   *
   * @param generation The image generation result from the AI provider.
   * @return The image URL, or {@code null} if not available.
   */
  private String extractUrl(org.springframework.ai.image.ImageGeneration generation) {
    if (generation == null || generation.getOutput() == null) return null;
    return generation.getOutput().getUrl();
  }

  /**
   * Decodes a base64-encoded image string into raw bytes.
   *
   * @param b64 The base64-encoded string (nullable).
   * @return The decoded bytes, or {@code null} if the input is blank or decoding fails.
   */
  private byte[] decodeBase64Image(String b64) {
    if (b64 == null || b64.isBlank()) return null;
    try {
      return Base64.getDecoder().decode(b64.trim());
    } catch (IllegalArgumentException e) {
      logger.warn("Failed to decode base64 image data", e);
      return null;
    }
  }

  /**
   * Reflective fallback for extracting image bytes when the model output uses a non-standard {@code
   * getImage()} method instead of the base64 API.
   *
   * @param output The raw image output object from the AI provider.
   * @return The raw image bytes, or {@code null} if the method does not exist.
   */
  private byte[] extractImageViaReflection(Object output) {
    try {
      var method = output.getClass().getMethod("getImage");
      Object imgObj = method.invoke(output);
      if (imgObj instanceof byte[] bytes) return bytes;
    } catch (Exception e) {
      // Method getImage() does not exist or failed — ignore
    }
    return null;
  }

  /**
   * Builds an RFC 2397 Data URL from raw PNG image bytes.
   *
   * @param imageData The raw PNG image bytes.
   * @return A {@code data:image/png;base64,...} string.
   */
  private String buildImageDataUrl(byte[] imageData) {
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
  }

  /**
   * Generates speech audio from text using the active TTS model.
   *
   * <p>Resolves the user's preferred voice from the request context's preferences (defaults to
   * {@code "alloy"}) and delegates to the registered {@link TextToSpeechModel}.
   *
   * @param request The speech request containing the text and user context.
   * @return The raw audio bytes (MP3 format).
   * @throws CoreException If no TextToSpeechModel is registered for the active provider.
   */
  public byte[] generateSpeech(InternalSpeechRequest request) {
    var voice = MediaPayloadHandler.resolveVoicePreference(request.context());
    var response =
        registry.getActiveSpeechModel().call(MediaPayloadHandler.toSpeechPrompt(request, voice));
    return response.getResult().getOutput();
  }
}
