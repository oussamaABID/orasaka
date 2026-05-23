package com.orasaka.core.model;

import com.orasaka.core.context.OrasakaContext;

/**
 * Unified image request record for multi-modal generation.
 *
 * @param prompt Descriptive text for the image to generate.
 * @param width Desired width in pixels (optional).
 * @param height Desired height in pixels (optional).
 * @param options Provider-specific generation options.
 * @param context The execution context carrying user preferences (e.g., aspect ratios).
 */
public record OrasakaImageRequest(
    String prompt, Integer width, Integer height, OrasakaOptions options, OrasakaContext context) {
  /** Compact constructor enforcing non-empty prompt. */
  public OrasakaImageRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt cannot be empty");
    }
  }
}
