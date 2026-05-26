package com.orasaka.core.domain.model.video;

import com.orasaka.core.domain.model.AiRequest;
import com.orasaka.core.domain.model.Context;
import java.util.Map;

/**
 * Immutable record carrying the prompt, parameters, and security context for Text-to-Video
 * generation.
 */
public record VideoRequest(
    String prompt,
    Integer durationSeconds,
    String model,
    String jobId,
    String inputPath,
    String outputPath,
    Map<String, Object> settings,
    Context context)
    implements AiRequest {
  private static final int DEFAULT_DURATION_SECONDS = 4;

  public VideoRequest {
    AiRequest.requireValid(prompt, context);
    durationSeconds = (durationSeconds != null) ? durationSeconds : DEFAULT_DURATION_SECONDS;
    settings = (settings != null) ? Map.copyOf(settings) : Map.of();
    model = (model != null) ? model : "";
    jobId = (jobId != null) ? jobId : "";
    inputPath = (inputPath != null) ? inputPath : "";
    outputPath = (outputPath != null) ? outputPath : "";
  }

  /** Overloaded constructor for 5-argument usage. */
  public VideoRequest(
      String prompt,
      Integer durationSeconds,
      String model,
      Map<String, Object> settings,
      Context context) {
    this(
        prompt,
        durationSeconds,
        model,
        settings != null ? (String) settings.getOrDefault("jobId", "") : "",
        settings != null ? (String) settings.getOrDefault("image_path", "") : "",
        settings != null ? (String) settings.getOrDefault("output_path", "") : "",
        settings,
        context);
  }

  /** Overloaded constructor for backward compatibility with 4-argument usages. */
  public VideoRequest(
      String prompt, Integer durationSeconds, Map<String, Object> settings, Context context) {
    this(prompt, durationSeconds, "", settings, context);
  }
}
