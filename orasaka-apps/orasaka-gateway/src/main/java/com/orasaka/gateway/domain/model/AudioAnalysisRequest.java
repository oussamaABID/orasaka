package com.orasaka.gateway.domain.model;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.UUID;

/**
 * Self-validating request DTO for audio analysis containing asset ID, thread ID, and optional model
 * name.
 */
public record AudioAnalysisRequest(UUID assetId, String threadId, String model) {
  public AudioAnalysisRequest {
    if (assetId == null) {
      throw new InvalidRequestException("assetId is required");
    }
    if (threadId == null || threadId.isBlank()) {
      threadId = UUID.randomUUID().toString();
    }
    if (model == null || model.isBlank()) {
      model = "whisper-1";
    }
  }
}
