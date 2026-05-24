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

/**
 * Spring Boot {@code @Configuration} class for the {@code orasaka-core} module.
 *
 * <p>Defines and assembles all core engine beans: AI model maps (chat, image, embedding, speech),
 * configuration properties ({@link CoreProperties}, {@link FeaturesProperties}), the {@link
 * GraphEngine}, and the {@link AdminRegistry}.
 *
 * <p>Model instantiation is delegated to {@link ModelFactory}, with a special case for LocalAI
 * image models that use the OpenAI-compatible API.
 *
 * @see ModelFactory
 * @see CoreProperties
 * @since 1.0.0
 */
@Configuration
public class OrasakaCoreConfiguration {

  /**
   * Creates the provider → {@link ChatModel} map bean.
   *
   * @param properties Core configuration with provider overrides.
   * @return Immutable map of provider name → ChatModel.
   */
  @Bean
  public Map<String, ChatModel> chatModels(CoreProperties properties) {
    return ModelFactory.createChatModels(properties);
  }

  /**
   * Creates the provider → {@link EmbeddingModel} map bean.
   *
   * @param properties Core configuration with provider overrides.
   * @return Immutable map of provider name → EmbeddingModel.
   */
  @Bean
  public Map<String, EmbeddingModel> embeddingModels(CoreProperties properties) {
    return ModelFactory.createEmbeddingModels(properties);
  }

  /**
   * Creates the provider → {@link ImageModel} map bean, including LocalAI fallback.
   *
   * <p>If a {@code localai} provider is configured, it is registered as both {@code "localai"} and
   * {@code "ollama"} to enable transparent fallback.
   *
   * @param properties Core configuration with provider overrides.
   * @return Mutable map of provider name → ImageModel.
   */
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

  /**
   * Creates an empty provider → {@link TextToSpeechModel} map bean.
   *
   * <p>TTS models are currently not auto-discovered; this provides a placeholder for manual or
   * conditional registration.
   *
   * @return An empty mutable map.
   */
  @Bean
  public Map<String, TextToSpeechModel> speechModels() {
    return new HashMap<>();
  }

  /**
   * Binds the {@code orasaka.core} configuration prefix to a {@link CoreProperties} record.
   *
   * @param env The Spring environment for property resolution.
   * @return The bound CoreProperties instance.
   * @throws IllegalStateException If the configuration prefix is missing or binding fails.
   */
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

  /**
   * Binds the {@code orasaka} configuration prefix to {@link FeaturesProperties}.
   *
   * @param env The Spring environment for property resolution.
   * @return The bound features properties, or a default empty instance.
   */
  @Bean
  public FeaturesProperties featuresProperties(Environment env) {
    return Binder.get(env)
        .bind("orasaka", FeaturesProperties.class)
        .orElseGet(() -> new FeaturesProperties(Map.of()));
  }

  /**
   * Creates the singleton {@link AdminRegistry} for runtime capability lock management.
   *
   * @return A new thread-safe admin registry instance.
   */
  @Bean
  public AdminRegistry adminRegistry() {
    return new AdminRegistry();
  }

  /**
   * Creates the {@link GraphEngine} bean for SDUI Operation Graph compilation.
   *
   * @param featuresProperties Static feature capability blueprints.
   * @param adminRegistry Runtime lock registry for dynamic capability control.
   * @return A configured graph engine instance.
   */
  @Bean
  public GraphEngine graphEngine(
      FeaturesProperties featuresProperties, AdminRegistry adminRegistry) {
    return new GraphEngine(featuresProperties, adminRegistry);
  }
}
