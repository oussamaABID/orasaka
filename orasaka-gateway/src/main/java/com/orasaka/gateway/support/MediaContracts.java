package com.orasaka.gateway.support;

import com.orasaka.identity.exception.InvalidRequestException;

/** Group of immutable media data contracts for multi-modal ingestion. */
public final class MediaContracts {

  private MediaContracts() {}

  /**
   * Request containing base64 encoded poster image.
   *
   * @param posterBase64 The base64 representation of the poster.
   * @param prompt Optional analysis prompt query.
   */
  public record AnalyzePosterRequest(String posterBase64, String prompt) {
    public AnalyzePosterRequest {
      if (posterBase64 == null || posterBase64.isBlank()) {
        throw new InvalidRequestException("posterBase64 is required");
      }
    }
  }

  /**
   * Request containing base64 encoded audio track clip.
   *
   * @param audioBase64 The base64 representation of the audio.
   * @param threadId The thread identifier.
   */
  public record AnalyzeAudioRequest(String audioBase64, String threadId) {
    public AnalyzeAudioRequest {
      if (audioBase64 == null || audioBase64.isBlank()) {
        throw new InvalidRequestException("audioBase64 is required");
      }
    }
  }
}
