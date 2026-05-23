package com.orasaka.core.engine;

import com.orasaka.core.pipeline.OrasakaContextInterceptor;
import com.orasaka.core.pipeline.OrasakaOrchestrationPipeline;
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

public abstract sealed class AbstractOrasakaEngine permits OrasakaEngine {

  private static final Logger logger = LoggerFactory.getLogger(AbstractOrasakaEngine.class);

  final EngineModelRegistry registry;
  final List<OrasakaContextInterceptor> interceptors;
  final OrasakaOrchestrationPipeline pipeline;
  final ApplicationEventPublisher eventPublisher;

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
    OpenAiImageOptions executionOptions =
        OpenAiImageOptions.builder().model("stable-diffusion").N(1).height(512).width(512).build();
    ImagePrompt prompt = new ImagePrompt(request.prompt(), executionOptions);
    var response = model.call(prompt);

    var generation = response.getResult();
    byte[] imageData = null;
    String url = null;

    if (generation != null && generation.getOutput() != null) {
      var output = generation.getOutput();
      url = output.getUrl();
      String b64 = output.getB64Json();
      if (b64 != null && !b64.isBlank()) {
        try {
          imageData = Base64.getDecoder().decode(b64.trim());
        } catch (IllegalArgumentException e) {
          logger.warn("Failed to decode base64 image data", e);
        }
      }

      if (imageData == null) {
        try {
          var method = output.getClass().getMethod("getImage");
          Object imgObj = method.invoke(output);
          if (imgObj instanceof byte[] bytes) {
            imageData = bytes;
          }
        } catch (Exception e) {
          // Method getImage() does not exist or failed, ignore
        }
      }
    }

    if (imageData != null && url == null) {
      url = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
    }

    return new OrasakaImageResponse(imageData, url, "png");
  }

  public byte[] generateSpeech(OrasakaSpeechRequest request) {
    var voice = MediaPayloadHandler.resolveVoicePreference(request.context());
    var response =
        registry.getActiveSpeechModel().call(MediaPayloadHandler.toSpeechPrompt(request, voice));
    return response.getResult().getOutput();
  }
}
