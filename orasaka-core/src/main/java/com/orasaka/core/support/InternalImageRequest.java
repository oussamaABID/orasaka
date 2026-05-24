package com.orasaka.core.support;

public record InternalImageRequest(
    String prompt, Integer width, Integer height, OrasakaOptions options, OrasakaContext context) {
  public InternalImageRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt cannot be empty");
    }
  }
}
