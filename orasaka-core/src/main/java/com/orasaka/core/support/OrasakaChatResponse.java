package com.orasaka.core.support;

import java.util.Map;

public record OrasakaChatResponse(
    String content, String conversationId, Map<String, Object> metadata) {
  public OrasakaChatResponse {
    if (content == null) {
      content = "";
    }
    metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
  }
}
