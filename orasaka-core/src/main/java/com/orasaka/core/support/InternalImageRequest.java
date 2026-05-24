package com.orasaka.core.support;

/**
 * Internal engine-level image generation request record.
 *
 * <p>Distinct from the public-facing {@link ImageRequest}, this is consumed directly by {@link
 * com.orasaka.core.engine.AbstractEngine#generateImage}.
 *
 * @param prompt Descriptive text prompt for image generation (required, non-blank).
 * @param width Desired image width in pixels (nullable — provider defaults apply).
 * @param height Desired image height in pixels (nullable — provider defaults apply).
 * @param options Provider-specific generation options (nullable).
 * @param context Execution context with user preferences (nullable).
 * @see InternalImageResponse
 * @since 1.0.0
 */
public record InternalImageRequest(
    String prompt, Integer width, Integer height, Options options, Context context) {
  /** Compact constructor — validates the prompt is non-null and non-blank. */
  public InternalImageRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt cannot be empty");
    }
  }
}
