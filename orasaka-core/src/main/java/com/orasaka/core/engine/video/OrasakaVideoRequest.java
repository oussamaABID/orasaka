package com.orasaka.core.engine.video;

import com.orasaka.core.support.OrasakaContext;
import java.util.Map;

/**
 * Immutable record carrying the prompt, parameters, and security context for Text-to-Video
 * generation.
 */
public record OrasakaVideoRequest(
    String prompt, Integer durationSeconds, Map<String, Object> settings, OrasakaContext context) {
  private static final int DEFAULT_DURATION_SECONDS = 4;

  public OrasakaVideoRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt cannot be null or empty");
    }
    durationSeconds = (durationSeconds != null) ? durationSeconds : DEFAULT_DURATION_SECONDS;
    settings = (settings != null) ? Map.copyOf(settings) : Map.of();
  }
}
