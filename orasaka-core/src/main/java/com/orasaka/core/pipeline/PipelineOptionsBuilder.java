package com.orasaka.core.pipeline;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Package-private utility for building provider-specific {@link ChatOptions} used by the pipeline
 * interceptors (refiner, router).
 *
 * @since 1.0.0
 */
class PipelineOptionsBuilder {

  /** Private constructor — utility class, not instantiable. */
  private PipelineOptionsBuilder() {}

  /**
   * Builds provider-specific {@link ChatOptions} from the given parameters.
   *
   * @param provider The provider name (e.g., {@code "ollama"}, {@code "openai"}).
   * @param modelName The model name to configure.
   * @param temp The temperature to use.
   * @return Provider-specific ChatOptions, or {@code null} for unknown providers.
   */
  static ChatOptions build(String provider, String modelName, Double temp) {
    return switch (provider.toLowerCase()) {
      case "ollama" -> OllamaChatOptions.builder().model(modelName).temperature(temp).build();
      case "openai" -> OpenAiChatOptions.builder().model(modelName).temperature(temp).build();
      default -> null;
    };
  }
}
