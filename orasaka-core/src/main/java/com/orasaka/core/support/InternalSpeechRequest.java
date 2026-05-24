package com.orasaka.core.support;

public record InternalSpeechRequest(String text, OrasakaOptions options, OrasakaContext context) {
  public InternalSpeechRequest {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Text cannot be empty");
    }
  }
}
