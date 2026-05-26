package com.orasaka.gateway.domain.model;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.UUID;

/** Group of immutable media data contracts for multi-modal ingestion. */
public final class MediaContracts {

  private MediaContracts() {}

  /**
   * Request containing assetId UUID referencing the uploaded poster image.
   *
   * @param assetId The unique asset ID.
   * @param prompt Optional analysis prompt query.
   */
  public record AnalyzePosterRequest(UUID assetId, String prompt, String model) {
    public AnalyzePosterRequest {
      if (assetId == null) {
        throw new InvalidRequestException("assetId is required");
      }
    }

    public AnalyzePosterRequest(UUID assetId, String prompt) {
      this(assetId, prompt, null);
    }
  }

  /**
   * Request containing assetId UUID referencing the uploaded audio track clip.
   *
   * @param assetId The unique asset ID.
   * @param threadId The thread identifier.
   */
  public record AnalyzeAudioRequest(UUID assetId, String threadId, String model) {
    public AnalyzeAudioRequest {
      if (assetId == null) {
        throw new InvalidRequestException("assetId is required");
      }
      if (threadId == null || threadId.isBlank()) {
        threadId = UUID.randomUUID().toString();
      }
    }

    public AnalyzeAudioRequest(UUID assetId, String threadId) {
      this(assetId, threadId, null);
    }
  }
}
