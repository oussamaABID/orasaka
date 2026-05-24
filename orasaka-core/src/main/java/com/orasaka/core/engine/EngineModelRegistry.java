package com.orasaka.core.engine;

import com.orasaka.core.support.CoreException;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;

/**
 * Centralized registry of AI provider model instances for the Orasaka engine.
 *
 * <p>This class holds immutable, defensive copies of all model maps (chat, image, embedding,
 * speech) keyed by provider name (e.g., {@code "ollama"}, {@code "openai"}). It provides resolution
 * methods that enforce fail-fast semantics — throwing {@link CoreException} when a required model
 * is not registered.
 *
 * <p>The registry is package-private and constructed exclusively by {@link AbstractEngine} at
 * startup time. It is <b>not</b> a Spring bean.
 *
 * @see AbstractEngine
 * @see ModelFactory
 * @since 1.0.0
 */
class EngineModelRegistry {

  /** Immutable map of provider name → ChatModel instances. */
  private final Map<String, ChatModel> chatModels;

  /** Immutable map of provider name → ImageModel instances. */
  private final Map<String, ImageModel> imageModels;

  /** Immutable map of provider name → EmbeddingModel instances. */
  private final Map<String, EmbeddingModel> embeddingModels;

  /** Immutable map of provider name → TextToSpeechModel instances. */
  private final Map<String, TextToSpeechModel> speechModels;

  /** Core configuration properties containing the default provider and overrides. */
  private final CoreProperties properties;

  /**
   * Constructs the registry with defensively copied model maps.
   *
   * @param chatModels Provider → ChatModel map (nullable — defaults to empty).
   * @param imageModels Provider → ImageModel map (nullable — defaults to empty).
   * @param embeddingModels Provider → EmbeddingModel map (nullable — defaults to empty).
   * @param speechModels Provider → TextToSpeechModel map (nullable — defaults to empty).
   * @param properties Core configuration properties for default provider resolution.
   */
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

  /**
   * Returns the configured default AI provider name.
   *
   * @return The non-blank provider identifier (e.g., {@code "ollama"}).
   * @throws CoreException If {@code orasaka.core.default-provider} is missing or blank.
   */
  String getActiveProvider() {
    return Optional.ofNullable(properties.defaultProvider())
        .filter(s -> !s.isBlank())
        .orElseThrow(
            () -> new CoreException("Missing required property: orasaka.core.default-provider"));
  }

  /**
   * Returns the base URL for the active provider's API endpoint.
   *
   * @return The non-blank base URL string.
   * @throws CoreException If the base URL is missing for the active provider.
   */
  String getBaseUrl() {
    String provider = getActiveProvider();
    return Optional.ofNullable(properties.overrides())
        .map(m -> m.get(provider))
        .map(CoreProperties.ProviderConfig::baseUrl)
        .filter(s -> !s.isBlank())
        .orElseThrow(
            () ->
                new CoreException(
                    "Missing required property: orasaka.core.overrides." + provider + ".base-url"));
  }

  /**
   * Resolves a {@link ChatModel} for the specified provider.
   *
   * @param provider The provider name (e.g., {@code "ollama"}, {@code "openai"}).
   * @return The registered ChatModel instance.
   * @throws CoreException If no ChatModel is registered for the given provider.
   */
  ChatModel getChatModel(String provider) {
    return Optional.ofNullable(chatModels.get(provider))
        .orElseThrow(() -> new CoreException("No ChatModel found for provider: " + provider));
  }

  /**
   * Resolves the {@link EmbeddingModel} for the active default provider.
   *
   * @return The registered EmbeddingModel instance.
   * @throws CoreException If no EmbeddingModel is registered for the active provider.
   */
  EmbeddingModel getActiveEmbeddingModel() {
    String provider = getActiveProvider();
    return Optional.ofNullable(embeddingModels.get(provider))
        .orElseThrow(() -> new CoreException("No EmbeddingModel found for provider: " + provider));
  }

  /**
   * Resolves the {@link ImageModel} for the active provider, falling back to {@code "openai"}.
   *
   * @return The registered ImageModel instance.
   * @throws CoreException If no ImageModel is registered for the active or fallback provider.
   */
  ImageModel getActiveImageModel() {
    String provider = getActiveProvider();
    return Optional.ofNullable(imageModels.get(provider))
        .or(() -> Optional.ofNullable(imageModels.get("openai")))
        .orElseThrow(
            () ->
                new CoreException(
                    "No ImageModel found for provider '"
                        + provider
                        + "'. Register an ImageModel bean or configure a valid provider."));
  }

  /**
   * Resolves the {@link TextToSpeechModel} for the active provider, falling back to {@code
   * "openai"}.
   *
   * @return The registered TextToSpeechModel instance.
   * @throws CoreException If no TextToSpeechModel is registered for the active or fallback
   *     provider.
   */
  TextToSpeechModel getActiveSpeechModel() {
    String provider = getActiveProvider();
    return Optional.ofNullable(speechModels.get(provider))
        .or(() -> Optional.ofNullable(speechModels.get("openai")))
        .orElseThrow(
            () ->
                new CoreException(
                    "No TextToSpeechModel found for provider '"
                        + provider
                        + "'. Register a TextToSpeechModel bean or configure a valid provider."));
  }
}
