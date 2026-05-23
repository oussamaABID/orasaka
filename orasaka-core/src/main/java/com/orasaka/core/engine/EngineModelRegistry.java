package com.orasaka.core.engine;

import com.orasaka.core.support.OrasakaException;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;

class EngineModelRegistry {
  private final Map<String, ChatModel> chatModels;
  private final Map<String, ImageModel> imageModels;
  private final Map<String, EmbeddingModel> embeddingModels;
  private final Map<String, TextToSpeechModel> speechModels;
  private final CoreProperties properties;

  EngineModelRegistry(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties) {
    this.chatModels = chatModels != null ? Map.copyOf(chatModels) : Map.of();
    this.imageModels = imageModels != null ? Map.copyOf(imageModels) : Map.of();
    this.embeddingModels = embeddingModels != null ? Map.copyOf(embeddingModels) : Map.of();
    this.speechModels = speechModels != null ? Map.copyOf(speechModels) : Map.of();
    this.properties = properties;
  }

  String getActiveProvider() {
    return Optional.ofNullable(properties.defaultProvider())
        .filter(s -> !s.isBlank())
        .orElseThrow(
            () -> new OrasakaException("Missing required property: orasaka.core.default-provider"));
  }

  String getBaseUrl() {
    String provider = getActiveProvider();
    return Optional.ofNullable(properties.overrides())
        .map(m -> m.get(provider))
        .map(CoreProperties.ProviderConfig::baseUrl)
        .filter(s -> !s.isBlank())
        .orElseThrow(
            () ->
                new OrasakaException(
                    "Missing required property: orasaka.core.overrides." + provider + ".base-url"));
  }

  ChatModel getChatModel(String provider) {
    return Optional.ofNullable(chatModels.get(provider))
        .orElseThrow(() -> new OrasakaException("No ChatModel found for provider: " + provider));
  }

  EmbeddingModel getActiveEmbeddingModel() {
    String provider = getActiveProvider();
    return Optional.ofNullable(embeddingModels.get(provider))
        .orElseThrow(
            () -> new OrasakaException("No EmbeddingModel found for provider: " + provider));
  }

  ImageModel getActiveImageModel() {
    String provider = getActiveProvider();
    ImageModel model = imageModels.get(provider);
    if (model == null) {
      model = imageModels.get("openai");
    }
    if (model == null) {
      model =
          new ImageModel() {
            @Override
            public org.springframework.ai.image.ImageResponse call(
                org.springframework.ai.image.ImagePrompt prompt) {
              var gen =
                  new org.springframework.ai.image.ImageGeneration(
                      new org.springframework.ai.image.Image(
                          "http://localhost:3000/placeholder.png", null));
              return new org.springframework.ai.image.ImageResponse(java.util.List.of(gen));
            }
          };
    }
    return model;
  }

  TextToSpeechModel getActiveSpeechModel() {
    String provider = getActiveProvider();
    TextToSpeechModel model = speechModels.get(provider);
    if (model == null) {
      model = speechModels.get("openai");
    }
    if (model == null) {
      model =
          new TextToSpeechModel() {
            @Override
            public org.springframework.ai.audio.tts.TextToSpeechResponse call(
                org.springframework.ai.audio.tts.TextToSpeechPrompt prompt) {
              return new org.springframework.ai.audio.tts.TextToSpeechResponse(
                  java.util.List.of(new org.springframework.ai.audio.tts.Speech(new byte[0])));
            }

            @Override
            public reactor.core.publisher.Flux<
                    org.springframework.ai.audio.tts.TextToSpeechResponse>
                stream(org.springframework.ai.audio.tts.TextToSpeechPrompt prompt) {
              return reactor.core.publisher.Flux.just(call(prompt));
            }
          };
    }
    return model;
  }
}
