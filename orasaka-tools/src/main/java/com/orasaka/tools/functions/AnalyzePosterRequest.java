package com.orasaka.tools.functions;

import java.util.Objects;

/** Request payload for analyzing movie posters using Vision capabilities. */
public record AnalyzePosterRequest(String posterBase64, String prompt) {
  public AnalyzePosterRequest {
    Objects.requireNonNull(posterBase64, "posterBase64 must not be null");
    if (posterBase64.isBlank()) {
      throw new IllegalArgumentException("posterBase64 must not be blank");
    }
    prompt =
        (prompt != null && !prompt.isBlank())
            ? prompt
            : "Analyze this movie poster for visual content and themes.";
  }
}
