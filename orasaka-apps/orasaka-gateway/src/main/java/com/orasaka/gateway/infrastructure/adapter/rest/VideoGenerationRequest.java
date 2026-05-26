package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.Map;

/** Request DTO for video generation containing prompt, duration, model and settings parameters. */
public record VideoGenerationRequest(
    String prompt,
    Integer durationSeconds,
    Integer numFrames,
    Integer videoFps,
    String model,
    String image,
    Map<String, Object> settings) {
  public VideoGenerationRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new InvalidRequestException("Prompt is required");
    }
    settings = (settings != null) ? Map.copyOf(settings) : Map.of();
  }
}
