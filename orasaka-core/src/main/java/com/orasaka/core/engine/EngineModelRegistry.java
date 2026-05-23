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

  ImageModel getActiveImageModel() {
    String provider = getActiveProvider();
    return Optional.ofNullable(imageModels.get(provider))
        .orElseThrow(() -> new OrasakaException("No ImageModel found for provider: " + provider));
  }

  TextToSpeechModel getActiveSpeechModel() {
    String provider = getActiveProvider();
    return Optional.ofNullable(speechModels.get(provider))
        .orElseThrow(
            () -> new OrasakaException("No TextToSpeechModel found for provider: " + provider));
  }
}
