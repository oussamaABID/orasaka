package com.orasaka.core.engine;

import com.orasaka.core.support.CoreException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;

/**
 * Static factory for constructing provider-specific Spring AI model instances.
 *
 * <p>Reads provider configurations from {@link CoreProperties} and instantiates the appropriate
 * {@link ChatModel}, {@link EmbeddingModel}, and {@link ImageModel} beans for each configured
 * provider (Ollama, OpenAI, LocalAI).
 *
 * <p>This factory is invoked exclusively by {@link OrasakaCoreConfiguration} during Spring Boot
 * auto-configuration. It is package-private and not a Spring bean itself.
 *
 * <p><b>Security</b>: API keys are validated as non-blank before use. Missing keys throw {@link
 * CoreException} with a clear security violation message.
 *
 * @see OrasakaCoreConfiguration
 * @see CoreProperties.ProviderConfig
 * @since 1.0.0
 */
final class ModelFactory {

  /** Private constructor — utility class, not instantiable. */
  private ModelFactory() {}

  /**
   * Creates a map of provider name → {@link ChatModel} instances from configuration.
   *
   * <p>Currently supports:
   *
   * <ul>
   *   <li>{@code "ollama"} — via {@link OllamaChatModel} with configurable model and temperature
   *   <li>{@code "openai"} — via {@link OpenAiChatModel} with API key validation
   * </ul>
   *
   * @param properties The core configuration containing provider overrides.
   * @return A mutable map of provider name → ChatModel (may be empty if no providers are
   *     configured).
   * @throws CoreException If a provider's required properties (base URL, API key) are missing.
   */
  static Map<String, ChatModel> createChatModels(CoreProperties properties) {
    Map<String, ChatModel> models = new HashMap<>();

    CoreProperties.ProviderConfig ollama = properties.overrides().get("ollama");
    if (ollama != null) {
      String baseUrl =
          Optional.ofNullable(ollama.baseUrl())
              .filter(s -> !s.isBlank())
              .orElseThrow(
                  () ->
                      new CoreException(
                          "Missing required property: orasaka.core.overrides.ollama.base-url"));
      String model = Optional.ofNullable(ollama.model()).orElse("llama3:8b");
      OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
      OllamaChatOptions.Builder opts = OllamaChatOptions.builder().model(model);
      Optional.ofNullable(ollama.temperature()).ifPresent(opts::temperature);
      models.put(
          "ollama", OllamaChatModel.builder().ollamaApi(api).defaultOptions(opts.build()).build());
    }

    CoreProperties.ProviderConfig openai = properties.overrides().get("openai");
    if (openai != null && openai.baseUrl() != null) {
      String apiKey =
          Optional.ofNullable(openai.apiKey())
              .filter(k -> !k.isBlank())
              .orElseThrow(
                  () ->
                      new CoreException(
                          "Critical Security Violation: OpenAI API Key is unresolved."
                              + " Check orasaka.core.overrides.openai.api-key configuration."));
      OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).baseUrl(openai.baseUrl()).build();
      models.put("openai", OpenAiChatModel.builder().openAiApi(api).build());
    }

    return models;
  }

  /**
   * Creates a map of provider name → {@link EmbeddingModel} instances from configuration.
   *
   * <p>Currently supports Ollama with a configurable embedding model name (defaults to {@code
   * "all-minilm"} if not specified in {@code extra.embedding-model}).
   *
   * @param properties The core configuration containing provider overrides.
   * @return A mutable map of provider name → EmbeddingModel.
   * @throws CoreException If the Ollama base URL is missing.
   */
  static Map<String, EmbeddingModel> createEmbeddingModels(CoreProperties properties) {
    Map<String, EmbeddingModel> models = new HashMap<>();

    CoreProperties.ProviderConfig ollama = properties.overrides().get("ollama");
    if (ollama != null) {
      String baseUrl =
          Optional.ofNullable(ollama.baseUrl())
              .filter(s -> !s.isBlank())
              .orElseThrow(
                  () ->
                      new CoreException(
                          "Missing required property: orasaka.core.overrides.ollama.base-url"));
      String model =
          Optional.ofNullable(ollama.extra())
              .map(e -> e.get("embedding-model"))
              .map(Object::toString)
              .orElse("all-minilm");
      OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
      OllamaEmbeddingOptions opts = OllamaEmbeddingOptions.builder().model(model).build();
      models.put(
          "ollama", OllamaEmbeddingModel.builder().ollamaApi(api).defaultOptions(opts).build());
    }

    return models;
  }

  /**
   * Creates a map of provider name → {@link ImageModel} instances from configuration.
   *
   * <p>Currently supports OpenAI image generation with API key validation.
   *
   * @param properties The core configuration containing provider overrides.
   * @return A mutable map of provider name → ImageModel.
   * @throws CoreException If the OpenAI API key is missing or blank.
   */
  static Map<String, ImageModel> createImageModels(CoreProperties properties) {
    Map<String, ImageModel> models = new HashMap<>();

    CoreProperties.ProviderConfig openai = properties.overrides().get("openai");
    if (openai != null && openai.baseUrl() != null) {
      String apiKey =
          Optional.ofNullable(openai.apiKey())
              .filter(k -> !k.isBlank())
              .orElseThrow(
                  () ->
                      new CoreException(
                          "Critical Security Violation: OpenAI API Key is unresolved."
                              + " Check orasaka.core.overrides.openai.api-key configuration."));
      OpenAiImageApi api =
          OpenAiImageApi.builder().apiKey(apiKey).baseUrl(openai.baseUrl()).build();
      models.put("openai", new OpenAiImageModel(api));
    }

    return models;
  }
}
