package com.orasaka.core.engine;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
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
    return ModelFactory.createImageModels(properties);
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
