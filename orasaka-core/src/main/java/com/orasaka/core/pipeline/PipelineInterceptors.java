package com.orasaka.core.pipeline;

import java.util.Optional;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * Utility class with shared helper methods for the Context-Matrix orchestration pipeline.
 *
 * <p>Provides the {@link #extractResponseText} utility used by both {@link RefinerInterceptor} and
 * {@link RouterInterceptor} to safely parse ChatModel responses.
 *
 * @see RefinerInterceptor
 * @see RouterInterceptor
 * @since 1.0.0
 */
public final class PipelineInterceptors {

  /** Private constructor — utility class, not instantiable. */
  private PipelineInterceptors() {}

  /**
   * Safely extracts the trimmed response text from a {@link ChatResponse}.
   *
   * @param response The ChatResponse from a model call (nullable).
   * @return An {@link Optional} containing the non-blank, stripped text, or empty.
   */
  static Optional<String> extractResponseText(ChatResponse response) {
    return Optional.ofNullable(response)
        .map(ChatResponse::getResult)
        .map(result -> result.getOutput())
        .map(output -> output.getText())
        .map(String::strip)
        .filter(text -> !text.isBlank());
  }
}
