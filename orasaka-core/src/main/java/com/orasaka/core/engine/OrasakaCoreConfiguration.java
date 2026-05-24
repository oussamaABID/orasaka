package com.orasaka.core.engine;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OrasakaCoreConfiguration {

  @Bean
  public Map<String, ChatModel> chatModels(CoreProperties properties) {
    return ModelFactory.createChatModels(properties);
  }

  @Bean
  public Map<String, EmbeddingModel> embeddingModels(CoreProperties properties) {
    return ModelFactory.createEmbeddingModels(properties);
  }

  @Bean
  public Map<String, ImageModel> imageModels(CoreProperties properties) {
    Map<String, ImageModel> models = new HashMap<>(ModelFactory.createImageModels(properties));
    if (properties.overrides() != null) {
      CoreProperties.ProviderConfig localai = properties.overrides().get("localai");
      if (localai != null && localai.baseUrl() != null) {
        String apiKey =
            (localai.apiKey() != null && !localai.apiKey().isBlank())
                ? localai.apiKey()
                : "not-required";
        OpenAiImageApi api =
            OpenAiImageApi.builder().apiKey(apiKey).baseUrl(localai.baseUrl()).build();
        OpenAiImageOptions defaultOptions =
            OpenAiImageOptions.builder()
                .model(localai.model() != null ? localai.model() : "stable-diffusion")
                .N(1)
                .height(512)
                .width(512)
                .build();
        OpenAiImageModel model =
            new OpenAiImageModel(
                api, defaultOptions, new org.springframework.retry.support.RetryTemplate());
        models.put("localai", model);
        models.put("ollama", model);
      }
    }
    return models;
  }

  @Bean
  public Map<String, TextToSpeechModel> speechModels() {
    return new HashMap<>();
  }

  @Bean
  public CoreProperties coreProperties(Environment env) {
    try {
      return Binder.get(env)
          .bind("orasaka.core", CoreProperties.class)
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "Configuration prefix 'orasaka.core' is missing in application.yml"));
    } catch (Exception e) {
      throw new IllegalStateException(
          "CRITICAL BINDING FAILURE: Verify that your application.yml fields match CoreProperties records exactly.",
          e);
    }
  }

  @Bean
  public OrasakaFeaturesProperties featuresProperties(Environment env) {
    return Binder.get(env)
        .bind("orasaka", OrasakaFeaturesProperties.class)
        .orElseGet(() -> new OrasakaFeaturesProperties(Map.of()));
  }

  @Bean
  public OrasakaAdminRegistry adminRegistry() {
    return new OrasakaAdminRegistry();
  }

  @Bean
  public OrasakaGraphEngine graphEngine(
      OrasakaFeaturesProperties featuresProperties, OrasakaAdminRegistry adminRegistry) {
    return new OrasakaGraphEngine(featuresProperties, adminRegistry);
  }
}
