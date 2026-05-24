package com.orasaka.core.engine.video;

import com.orasaka.core.context.OrasakaContext;
import java.util.Map;

/**
 * Immutable record carrying the prompt, parameters, and security context for Text-to-Video
 * generation.
 */
public record OrasakaVideoRequest(
    String prompt, Integer durationSeconds, Map<String, Object> settings, OrasakaContext context) {
  public OrasakaVideoRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt cannot be null or empty");
    }
    settings = (settings != null) ? Map.copyOf(settings) : Map.of();
  }
}
