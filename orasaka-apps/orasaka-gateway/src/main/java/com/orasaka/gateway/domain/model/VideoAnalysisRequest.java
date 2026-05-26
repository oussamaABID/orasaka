package com.orasaka.gateway.domain.model;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.UUID;

/** Self-validating request DTO for video analysis containing asset ID and optional model name. */
public record VideoAnalysisRequest(UUID assetId, String model) {
  public VideoAnalysisRequest {
    if (assetId == null) {
      throw new InvalidRequestException("assetId is required");
    }
    if (model == null || model.isBlank()) {
      model = "whisper-1";
    }
  }
}
