package com.orasaka.core.support;

public record OrasakaImageRequest(
    String prompt, Integer width, Integer height, OrasakaOptions options, OrasakaContext context) {
  public OrasakaImageRequest {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt cannot be empty");
    }
  }
}
