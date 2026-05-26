package com.orasaka.core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.core.application.engine.GraphEngine;
import com.orasaka.core.application.engine.InfrastructureProber;
import com.orasaka.core.application.processing.AudioPreProcessor;
import com.orasaka.core.application.processing.VideoPreProcessor;
import com.orasaka.core.domain.ports.outbound.AdminRegistry;
import com.orasaka.core.domain.ports.outbound.FeatureToggleProvider;
import com.orasaka.core.domain.ports.outbound.ModelCatalogProvider;
import com.orasaka.core.infrastructure.adapter.processor.LocalAudioProcessor;
import com.orasaka.core.infrastructure.adapter.processor.LocalVideoProcessor;
import com.orasaka.core.infrastructure.adapter.processor.WhisperTranscriptionClient;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

/**
 * Spring Boot {@code @Configuration} class for the {@code orasaka-core} module.
 *
 * <p>Defines configuration properties ({@link CoreProperties}, {@link FeaturesProperties}), the
 * {@link GraphEngine}, and infrastructure beans. AI model beans are in {@link
 * AiModelConfiguration}.
 *
 * @see AiModelConfiguration
 * @see CoreProperties
 * @since 1.0.0
 */
@Configuration
public class CoreConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(CoreConfiguration.class);

  private static final String DEFAULT_PROVIDER = "ollama";
  private static final String DEFAULT_REFINER_MODEL = "llama3.2:3b";

  /** Nested configuration record for LocalAI models. */
  public static record LocalAiConfig(String baseUrl, String apiKey, String model) {}

  /** Nested configuration record for custom image models. */
  public static record CustomImageConfig(String provider, String baseUrl) {}

  /** Nested configuration record for custom video models. */
  public static record CustomVideoConfig(String provider, String baseUrl) {}

  /**
   * Binds the LocalAI configuration record from the Spring environment.
   *
   * @param env Spring environment.
   * @return The bound LocalAiConfig bean.
   */
  @Bean
  public LocalAiConfig localAiConfig(Environment env) {
    LocalAiConfig localai =
        Binder.get(env)
            .bind("spring.ai.localai", LocalAiConfig.class)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Required configuration prefix 'spring.ai.localai' is missing."));
    if (localai.baseUrl() == null || localai.baseUrl().isBlank()) {
      throw new IllegalStateException(
          "Required configuration property 'spring.ai.localai.base-url' is missing.");
    }
    if (localai.apiKey() == null || localai.apiKey().isBlank()) {
      throw new IllegalStateException(
          "Required configuration property 'spring.ai.localai.api-key' is missing.");
    }
    if (localai.model() == null || localai.model().isBlank()) {
      throw new IllegalStateException(
          "Required configuration property 'spring.ai.localai.model' is missing.");
    }
    return localai;
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
    CoreProperties temp = null;
    try {
      temp = Binder.get(env).bind("orasaka.core", CoreProperties.class).orElse(null);
    } catch (RuntimeException e) {
      logger.debug("Could not bind 'orasaka.core' properties, using defaults.", e);
    }

    String defaultProvider =
        (temp != null && temp.defaultProvider() != null)
            ? temp.defaultProvider()
            : DEFAULT_PROVIDER;

    CoreProperties.RagConfig rag =
        (temp != null && temp.rag() != null)
            ? temp.rag()
            : new CoreProperties.RagConfig(true, "pgvector", 3);

    CoreProperties.McpConfig mcp =
        (temp != null && temp.mcp() != null) ? temp.mcp() : new CoreProperties.McpConfig(List.of());

    CoreProperties.OrchestrationConfig orchestration = null;
    if (temp != null && temp.orchestration() != null) {
      orchestration = temp.orchestration();
    } else {
      orchestration =
          new CoreProperties.OrchestrationConfig(
              true,
              new CoreProperties.UserContextConfig(true),
              new CoreProperties.SystemContextConfig(true),
              new CoreProperties.InterceptorConfig(
                  true, DEFAULT_PROVIDER, DEFAULT_REFINER_MODEL, 0.7),
              new CoreProperties.InterceptorConfig(
                  true, DEFAULT_PROVIDER, DEFAULT_REFINER_MODEL, 0.0));
    }

    CoreProperties.VideoGenerationConfig videoGen =
        new CoreProperties.VideoGenerationConfig(
            "localai-video", "http://" + "local" + "host:8188");
    CoreProperties.VideoAnalysisConfig videoAnalysis = new CoreProperties.VideoAnalysisConfig(8, 5);
    CoreProperties.VideoConfig videoConfig =
        new CoreProperties.VideoConfig(videoAnalysis, videoGen);

    // Default ImageConfig with defaults
    CoreProperties.ImageGenerationConfig imageGen =
        new CoreProperties.ImageGenerationConfig(
            "localai-image",
            "http://" + "local" + "host:8086",
            "not-required",
            "stable-diffusion",
            512,
            512,
            1,
            180000,
            180000);
    CoreProperties.ImageConfig imageConfig = new CoreProperties.ImageConfig(imageGen);

    CoreProperties.VisionConfig vision =
        new CoreProperties.VisionConfig(DEFAULT_PROVIDER, "llama3.2-vision:latest");
    CoreProperties.AudioConfig audio =
        new CoreProperties.AudioConfig(DEFAULT_PROVIDER, DEFAULT_REFINER_MODEL, "whisper-1");

    return new CoreProperties(
        defaultProvider, rag, mcp, orchestration, videoConfig, imageConfig, vision, audio);
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
   * Registers the shared {@link WhisperTranscriptionClient} bean for audio/video transcription.
   *
   * <p>Uses Spring {@link RestClient} with {@link org.springframework.core.io.ByteArrayResource}
   * for multipart file uploads — zero manual boundary forging.
   *
   * @param restClientBuilder The auto-configured RestClient builder.
   * @param objectMapper The auto-configured Jackson ObjectMapper.
   * @return A configured WhisperTranscriptionClient instance.
   */
  @Bean
  public WhisperTranscriptionClient whisperTranscriptionClient(
      RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
    return new WhisperTranscriptionClient(restClientBuilder, objectMapper);
  }

  /**
   * Registers the VideoPreProcessor bean.
   *
   * @param properties Core configuration properties.
   * @param catalogModelManager The catalog model manager.
   * @param whisperClient The shared Whisper transcription client.
   * @return A VideoPreProcessor instance.
   */
  @Bean
  public VideoPreProcessor videoPreProcessor(
      CoreProperties properties,
      CatalogModelManager catalogModelManager,
      WhisperTranscriptionClient whisperClient) {
    return new LocalVideoProcessor(properties, catalogModelManager, whisperClient);
  }

  /**
   * Registers the AudioPreProcessor bean.
   *
   * @param properties Core configuration properties.
   * @param catalogModelManager The catalog model manager.
   * @param whisperClient The shared Whisper transcription client.
   * @return An AudioPreProcessor instance.
   */
  @Bean
  public AudioPreProcessor audioPreProcessor(
      CoreProperties properties,
      CatalogModelManager catalogModelManager,
      WhisperTranscriptionClient whisperClient) {
    return new LocalAudioProcessor(properties, catalogModelManager, whisperClient);
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
   * @param featureToggleProvider Provider for database overrides on feature toggles.
   * @return A configured graph engine instance.
   */
  @Bean
  public GraphEngine graphEngine(
      FeaturesProperties featuresProperties,
      AdminRegistry adminRegistry,
      ObjectProvider<FeatureToggleProvider> featureToggleProvider,
      InfrastructureProber prober,
      ModelCatalogProvider modelCatalogProvider) {
    return new GraphEngine(
        featuresProperties,
        adminRegistry,
        featureToggleProvider.getIfAvailable(),
        prober,
        modelCatalogProvider);
  }

  /**
   * Registers the VectorStore bean.
   *
   * @param embeddingModelProvider Provider for EmbeddingModel.
   * @return The SimpleVectorStore bean.
   */
  @Bean
  @Conditional(OnMissingVectorStoreCondition.class)
  public VectorStore simpleVectorStore(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
    EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
    if (embeddingModel == null) {
      logger.warn("EmbeddingModel not available. SimpleVectorStore cannot be initialized.");
      return null;
    }
    return SimpleVectorStore.builder(embeddingModel).build();
  }
}
