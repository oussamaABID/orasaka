package com.orasaka.tools.functions;

import java.util.Objects;

/** Request payload for checking film audio extracts. */
public record AnalyzeAudioExtractRequest(String clipPath, String checkType) {
  public AnalyzeAudioExtractRequest {
    Objects.requireNonNull(clipPath, "clipPath must not be null");
    if (clipPath.isBlank()) {
      throw new IllegalArgumentException("clipPath must not be blank");
    }
    checkType = (checkType != null && !checkType.isBlank()) ? checkType : "compliance";
  }
}
