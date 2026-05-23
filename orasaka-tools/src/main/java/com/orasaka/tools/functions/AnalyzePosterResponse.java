package com.orasaka.tools.functions;

import java.util.Objects;

/** Response returned from movie poster Vision analysis. */
public record AnalyzePosterResponse(String analysis, boolean success) {
  public AnalyzePosterResponse {
    Objects.requireNonNull(analysis, "analysis must not be null");
  }
}
