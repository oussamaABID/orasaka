package com.orasaka.core.ingest.video;

import com.orasaka.core.support.Context;
import java.util.Map;

/**
 * Immutable record carrying the prompt, parameters, and security context for Text-to-Video
 * generation.
 */
public record VideoRequest(
    String prompt, Integer durationSeconds, Map<String, Object> settings, Context context) {
  private static final int DEFAULT_DURATION_SECONDS = 4;

  public VideoRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt cannot be null or empty");
    }
    durationSeconds = (durationSeconds != null) ? durationSeconds : DEFAULT_DURATION_SECONDS;
    settings = (settings != null) ? Map.copyOf(settings) : Map.of();
  }
}
