package com.orasaka.core.support;

public record OrasakaSpeechRequest(String text, OrasakaOptions options, OrasakaContext context) {
  public OrasakaSpeechRequest {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Text cannot be empty");
    }
  }
}
