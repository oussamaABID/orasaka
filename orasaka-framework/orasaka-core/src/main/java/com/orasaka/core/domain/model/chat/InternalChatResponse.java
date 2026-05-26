package com.orasaka.core.domain.model.chat;

import java.util.Map;

/**
 * Internal engine-level chat response record.
 *
 * <p>Wraps the generated text content, conversation thread ID, and provider metadata. Used both for
 * synchronous single responses and for individual streaming chunks.
 *
 * @param content The generated text content (defaults to empty string if null).
 * @param conversationId The session identifier for conversation tracking (nullable).
 * @param metadata Provider-specific metadata (e.g., model name, token usage).
 * @see InternalChatRequest
 */
public record InternalChatResponse(
    String content, String conversationId, Map<String, Object> metadata) {
  /** Compact constructor — defaults null content to empty string and copies metadata. */
  public InternalChatResponse {
    if (content == null) {
      content = "";
    }
    metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
  }
}
