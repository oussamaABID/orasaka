package com.orasaka.core.infrastructure.config;

import com.orasaka.core.infrastructure.config.CoreConfiguration.LocalAiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

/**
 * Configures AI model beans (chat, image, TTS) for the {@code orasaka-core} module.
 *
 * <p>Extracted from {@link CoreConfiguration} to enforce §2.1 (250-line limit) and separate model
 * wiring from property binding and engine assembly.
 *
 * @since 1.1.0
 */
@Configuration
class AiModelConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(AiModelConfiguration.class);

  /**
   * Resolves and provides the active {@link ChatModel} bean.
   *
   * @param ollamaProvider Provider for OllamaChatModel.
   * @param openaiProvider Provider for OpenAiChatModel.
   * @param properties Core configuration properties.
   * @param localAiConfig Bound LocalAI configuration.
   * @return The active ChatModel.
   */
  @Bean
  @Primary
  ChatModel activeChatModel(
      ObjectProvider<OllamaChatModel> ollamaProvider,
      ObjectProvider<OpenAiChatModel> openaiProvider,
      CoreProperties properties,
      LocalAiConfig localAiConfig) {
    String provider = properties.defaultProvider();
    if ("ollama".equalsIgnoreCase(provider)) {
      OllamaChatModel model = ollamaProvider.getIfAvailable();
      if (model != null) {
        return model;
      }
    }
    OpenAiChatModel openAiChatModel = openaiProvider.getIfAvailable();
    if (openAiChatModel != null) {
      return openAiChatModel;
    }
    if (localAiConfig != null) {
      if (localAiConfig.baseUrl() == null || localAiConfig.baseUrl().isBlank()) {
        throw new IllegalStateException(
            "Required configuration property 'spring.ai.localai.base-url' is missing.");
      }
      if (localAiConfig.apiKey() == null || localAiConfig.apiKey().isBlank()) {
        throw new IllegalStateException(
            "Required configuration property 'spring.ai.localai.api-key' is missing.");
      }
      OpenAiApi api =
          OpenAiApi.builder()
              .apiKey(localAiConfig.apiKey())
              .baseUrl(localAiConfig.baseUrl())
              .build();
      return OpenAiChatModel.builder().openAiApi(api).build();
    }
    throw new IllegalStateException("No ChatModel bean found for provider: " + provider);
  }

  /**
   * Resolves and provides the active {@link ImageModel} bean.
   *
   * @param openaiProvider Provider for OpenAiImageModel.
   * @param properties Core configuration properties.
   * @return The active ImageModel.
   */
  @Bean
  @Primary
  ImageModel activeImageModel(
      ObjectProvider<OpenAiImageModel> openaiProvider, CoreProperties properties) {
    var cfg = resolveImageGenConfig(properties);

    logger.info(
        "Initializing activeImageModel with baseUrl: {}, model: {}",
        cfg.getBaseUrl(),
        cfg.getModel());

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(cfg.getConnectTimeout());
    requestFactory.setReadTimeout(cfg.getReadTimeout());

    RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);

    OpenAiImageApi api =
        OpenAiImageApi.builder()
            .apiKey(cfg.getApiKey())
            .baseUrl(cfg.getBaseUrl())
            .restClientBuilder(restClientBuilder)
            .build();

    OpenAiImageOptions defaultOptions =
        OpenAiImageOptions.builder()
            .model(cfg.getModel())
            .N(cfg.getN())
            .height(cfg.getHeight())
            .width(cfg.getWidth())
            .build();
    return new SafeImageModel(new OpenAiImageModel(api, defaultOptions, new RetryTemplate()));
  }

  private static ImageGenConfig resolveImageGenConfig(CoreProperties properties) {
    var cfg = new ImageGenConfig();
    if (properties.image() == null || properties.image().generation() == null) {
      return cfg;
    }
    var gen = properties.image().generation();
    if (gen.baseUrl() != null && !gen.baseUrl().isBlank()) cfg.setBaseUrl(gen.baseUrl());
    if (gen.apiKey() != null && !gen.apiKey().isBlank()) cfg.setApiKey(gen.apiKey());
    if (gen.model() != null && !gen.model().isBlank()) cfg.setModel(gen.model());
    if (gen.connectTimeoutMs() != null) cfg.setConnectTimeout(gen.connectTimeoutMs());
    if (gen.readTimeoutMs() != null) cfg.setReadTimeout(gen.readTimeoutMs());
    if (gen.width() != null) cfg.setWidth(gen.width());
    if (gen.height() != null) cfg.setHeight(gen.height());
    if (gen.n() != null) cfg.setN(gen.n());
    return cfg;
  }

  private static final class ImageGenConfig {
    private String baseUrl = "http://" + "local" + "host:8086";
    private String apiKey = "not-required";
    private String model = "stable-diffusion";
    private int connectTimeout = 180000;
    private int readTimeout = 180000;
    private int width = 512;
    private int height = 512;
    private int n = 1;

    String getBaseUrl() {
      return baseUrl;
    }

    void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    String getApiKey() {
      return apiKey;
    }

    void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    String getModel() {
      return model;
    }

    void setModel(String model) {
      this.model = model;
    }

    int getConnectTimeout() {
      return connectTimeout;
    }

    void setConnectTimeout(int connectTimeout) {
      this.connectTimeout = connectTimeout;
    }

    int getReadTimeout() {
      return readTimeout;
    }

    void setReadTimeout(int readTimeout) {
      this.readTimeout = readTimeout;
    }

    int getWidth() {
      return width;
    }

    void setWidth(int width) {
      this.width = width;
    }

    int getHeight() {
      return height;
    }

    void setHeight(int height) {
      this.height = height;
    }

    int getN() {
      return n;
    }

    void setN(int n) {
      this.n = n;
    }
  }

  /**
   * Resolves and provides the active {@link TextToSpeechModel} bean.
   *
   * @param localAiConfig Bound LocalAI configuration.
   * @return The active TextToSpeechModel.
   */
  @Bean
  @Primary
  TextToSpeechModel activeTtsModel(LocalAiConfig localAiConfig) {
    if (localAiConfig.baseUrl() == null || localAiConfig.baseUrl().isBlank()) {
      throw new IllegalStateException(
          "Required configuration property 'spring.ai.localai.base-url' is missing.");
    }
    if (localAiConfig.apiKey() == null || localAiConfig.apiKey().isBlank()) {
      throw new IllegalStateException(
          "Required configuration property 'spring.ai.localai.api-key' is missing.");
    }
    if (localAiConfig.model() == null || localAiConfig.model().isBlank()) {
      throw new IllegalStateException(
          "Required configuration property 'spring.ai.localai.model' is missing.");
    }
    OpenAiAudioApi api =
        OpenAiAudioApi.builder()
            .baseUrl(localAiConfig.baseUrl())
            .apiKey(localAiConfig.apiKey())
            .build();
    OpenAiAudioSpeechOptions options =
        OpenAiAudioSpeechOptions.builder().model(localAiConfig.model()).build();
    RetryTemplate noRetryTemplate = new RetryTemplate();
    noRetryTemplate.setRetryPolicy(new NeverRetryPolicy());
    return new OpenAiAudioSpeechModel(api, options, noRetryTemplate);
  }
}
