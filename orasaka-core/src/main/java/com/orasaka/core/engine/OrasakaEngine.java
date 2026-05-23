package com.orasaka.core.engine;

import com.orasaka.core.pipeline.OrasakaContextInterceptor;
import com.orasaka.core.pipeline.OrasakaOrchestrationPipeline;
import java.util.List;
import java.util.Map;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Concrete, final implementation of {@link AbstractOrasakaEngine}.
 *
 * <p>This is the sole permitted subclass of the sealed {@link AbstractOrasakaEngine} hierarchy. It
 * delegates all orchestration logic to the abstract parent and serves as the default production
 * engine.
 */
@Component
public final class OrasakaEngine extends AbstractOrasakaEngine {

  /** Concrete constructor for OrasakaEngine. */
  @Autowired
  public OrasakaEngine(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties,
      List<OrasakaContextInterceptor> interceptors,
      OrasakaOrchestrationPipeline orchestrationPipeline,
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

  /** Backward-compatible constructor for OrasakaEngine. */
  public OrasakaEngine(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties,
      List<OrasakaContextInterceptor> interceptors,
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
