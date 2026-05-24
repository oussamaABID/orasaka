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
    eventPublisher.publishEvent(new OrasakaChatCompletedEvent(request, out));
    return out;
  }

  public Flux<InternalChatResponse> stream(InternalChatRequest request) {
    return EngineStreamBridge.createStream(
        request, pipeline, registry, interceptors, eventPublisher);
  }

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

  private byte[] extractImageData(org.springframework.ai.image.ImageGeneration generation) {
    if (generation == null || generation.getOutput() == null) return null;

    byte[] data = decodeBase64Image(generation.getOutput().getB64Json());
    if (data == null) {
      data = extractImageViaReflection(generation.getOutput());
    }
    return data;
  }

  private String extractUrl(org.springframework.ai.image.ImageGeneration generation) {
    if (generation == null || generation.getOutput() == null) return null;
    return generation.getOutput().getUrl();
  }

  private byte[] decodeBase64Image(String b64) {
    if (b64 == null || b64.isBlank()) return null;
    try {
      return Base64.getDecoder().decode(b64.trim());
    } catch (IllegalArgumentException e) {
      logger.warn("Failed to decode base64 image data", e);
      return null;
    }
  }

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

  private String buildImageDataUrl(byte[] imageData) {
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
  }

  public byte[] generateSpeech(InternalSpeechRequest request) {
    var voice = MediaPayloadHandler.resolveVoicePreference(request.context());
    var response =
        registry.getActiveSpeechModel().call(MediaPayloadHandler.toSpeechPrompt(request, voice));
    return response.getResult().getOutput();
  }
}
