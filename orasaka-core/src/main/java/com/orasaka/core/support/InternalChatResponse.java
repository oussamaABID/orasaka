package com.orasaka.core.support;

import java.util.Map;

public record InternalChatResponse(
    String content, String conversationId, Map<String, Object> metadata) {
  public InternalChatResponse {
    if (content == null) {
      content = "";
    }
    metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
  }
}
