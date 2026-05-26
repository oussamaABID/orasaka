package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;

/** Request DTO for text-to-speech generation containing text prompt and model parameters. */
public record TtsRequest(String text, String model, String voice) {
  public TtsRequest {
    if (text == null || text.isBlank()) {
      throw new InvalidRequestException("Text is required");
    }
  }
}
