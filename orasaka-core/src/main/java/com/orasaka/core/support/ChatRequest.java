package com.orasaka.core.support;

import java.util.List;

/**
 * Unified chat request record for all AI interactions.
 *
 * <p>Supports both single-turn prompt and multi-turn message history.
 *
 * @param prompt The current user input.
 * @param messages The conversation history as a list of {@link ChatMessage}.
 * @param options AI-specific configuration options.
 * @param context The execution context carrying session and user preferences.
 */
public record ChatRequest(
    String prompt, List<ChatMessage> messages, Options options, Context context) {
  /**
   * Represents an individual message in a chat history.
   *
   * @param role The role of the sender (e.g., "system", "user", "assistant").
   * @param content The text content of the message.
   */
  public record ChatMessage(String role, String content) {}

  /**
   * Utility factory for creating a simple chat request without history or options.
   *
   * @param prompt The user input.
   * @return A configured {@link ChatRequest}.
   */
  public static ChatRequest simple(String prompt) {
    return new ChatRequest(prompt, List.of(), null, null);
  }
}
