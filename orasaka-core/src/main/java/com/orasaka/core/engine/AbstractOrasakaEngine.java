package com.orasaka.core.engine;

import com.orasaka.core.pipeline.OrasakaContextInterceptor;
import com.orasaka.core.pipeline.OrasakaOrchestrationPipeline;
import com.orasaka.core.support.*;
import java.util.List;
import java.util.Map;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

public abstract sealed class AbstractOrasakaEngine permits OrasakaEngine {

  protected final EngineModelRegistry registry;
  protected final List<OrasakaContextInterceptor> interceptors;
  protected final OrasakaOrchestrationPipeline pipeline;
  protected final ApplicationEventPublisher eventPublisher;

  protected AbstractOrasakaEngine(
      Map<String, ChatModel> chat,
      Map<String, ImageModel> img,
      Map<String, EmbeddingModel> emb,
      Map<String, TextToSpeechModel> tts,
      CoreProperties props,
      List<OrasakaContextInterceptor> interceptors,
      OrasakaOrchestrationPipeline pipeline,
      ApplicationEventPublisher publisher) {
    this.registry = new EngineModelRegistry(chat, img, emb, tts, props);
    this.interceptors = interceptors != null ? List.copyOf(interceptors) : List.of();
    this.pipeline = pipeline;
    this.eventPublisher = publisher;
  }

  public OrasakaChatResponse chat(OrasakaChatRequest request) {
    var context = EnginePipelineBridge.compileContext(request, pipeline, registry, interceptors);
    var response = registry.getChatModel(context.provider()).call(context.toPrompt());
    var text = response.getResult().getOutput().getText();

    interceptors.forEach(i -> i.postProcess(request, context.promptText(), text));
    var out =
        new OrasakaChatResponse(
            text, context.conversationId(), Map.of("provider", context.provider()));
    eventPublisher.publishEvent(new OrasakaChatCompletedEvent(request, out));
    return out;
  }

  public Flux<OrasakaChatResponse> stream(OrasakaChatRequest request) {
    return EngineStreamBridge.createStream(
        request, pipeline, registry, interceptors, eventPublisher);
  }

  public OrasakaImageResponse generateImage(OrasakaImageRequest request) {
    var model = registry.getActiveImageModel();
    var response =
        model.call(MediaPayloadHandler.toImagePrompt(request, registry.getActiveProvider()));
    return new OrasakaImageResponse(null, response.getResult().getOutput().getUrl(), "png");
  }

  public byte[] generateSpeech(OrasakaSpeechRequest request) {
    var voice = MediaPayloadHandler.resolveVoicePreference(request.context());
    var response =
        registry.getActiveSpeechModel().call(MediaPayloadHandler.toSpeechPrompt(request, voice));
    return response.getResult().getOutput();
  }
}
