package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;

/** Request DTO for image generation containing prompt and model parameters. */
public record ImageGenerationRequest(String prompt, String model) {
  public ImageGenerationRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new InvalidRequestException("Prompt is required");
    }
  }
}
