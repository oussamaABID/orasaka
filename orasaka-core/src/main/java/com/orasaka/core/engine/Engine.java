package com.orasaka.core.engine;

import com.orasaka.core.pipeline.ContextInterceptor;
import com.orasaka.core.pipeline.OrchestrationPipeline;
import java.util.List;
import java.util.Map;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Concrete, final implementation of {@link AbstractEngine}.
 *
 * <p>This is the sole permitted subclass of the sealed {@link AbstractEngine} hierarchy. It
 * delegates all orchestration logic to the abstract parent and serves as the default production
 * engine.
 */
@Component
public final class Engine extends AbstractEngine {

  /**
   * Primary constructor for Engine with full dependency injection.
   *
   * @param chatModels Provider → ChatModel instances.
   * @param imageModels Provider → ImageModel instances.
   * @param embeddingModels Provider → EmbeddingModel instances.
   * @param speechModels Provider → TextToSpeechModel instances.
   * @param properties Core engine configuration properties.
   * @param interceptors Ordered list of context interceptors.
   * @param orchestrationPipeline The orchestration pipeline (nullable).
   * @param eventPublisher Spring event publisher for domain events.
   */
  public Engine(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties,
      List<ContextInterceptor> interceptors,
      OrchestrationPipeline orchestrationPipeline,
      ApplicationEventPublisher eventPublisher) {
    super(
        chatModels,
        imageModels,
        embeddingModels,
        speechModels,
        properties,
        interceptors,
        orchestrationPipeline,
        eventPublisher);
  }

  /**
   * Backward-compatible constructor without orchestration pipeline.
   *
   * <p>Delegates to the primary constructor with a {@code null} pipeline, which triggers the
   * zero-allocation bypass path.
   *
   * @param chatModels Provider → ChatModel instances.
   * @param imageModels Provider → ImageModel instances.
   * @param embeddingModels Provider → EmbeddingModel instances.
   * @param speechModels Provider → TextToSpeechModel instances.
   * @param properties Core engine configuration properties.
   * @param interceptors Ordered list of context interceptors.
   * @param eventPublisher Spring event publisher for domain events.
   */
  public Engine(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties,
      List<ContextInterceptor> interceptors,
      ApplicationEventPublisher eventPublisher) {
    this(
        chatModels,
        imageModels,
        embeddingModels,
        speechModels,
        properties,
        interceptors,
        null,
        eventPublisher);
  }
}
