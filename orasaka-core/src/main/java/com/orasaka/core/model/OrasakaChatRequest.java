package com.orasaka.core.model;

import com.orasaka.core.context.OrasakaContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

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
public record OrasakaChatRequest(
    String prompt, List<ChatMessage> messages, OrasakaOptions options, OrasakaContext context) {

  /** Compact constructor enforcing defensive copies. */
  public OrasakaChatRequest {
    messages = (messages == null) ? List.of() : List.copyOf(messages);
  }

  /**
   * Compiles history messages and the current user input into Spring AI's native representations.
   *
   * @param refinedPrompt Refined input text.
   * @param messageMapper Mapping function to translate ChatMessage to Message.
   * @return Immutable list of unified Message objects.
   */
  public List<Message> compileMessages(
      String refinedPrompt, Function<ChatMessage, Message> messageMapper) {
    List<Message> container = new ArrayList<>();
    if (!this.messages.isEmpty()) {
      container.addAll(this.messages.stream().map(messageMapper).toList());
    }
    if (refinedPrompt != null && !refinedPrompt.isBlank()) {
      container.add(new UserMessage(refinedPrompt));
    }
    return List.copyOf(container);
  }

  /**
   * Represents an individual message in a chat history.
   *
   * @param role The role of the sender (e.g., "system", "user", "assistant").
   * @param content The text content of the message.
   */
  public record ChatMessage(String role, String content) {
    /** Compact constructor enforcing non-empty role and non-null content. */
    public ChatMessage {
      if (role == null || role.isBlank()) {
        throw new IllegalArgumentException("Role cannot be empty");
      }
      if (content == null) {
        content = "";
      }
    }
  }

  /**
   * Utility factory for creating a simple chat request without history or options.
   *
   * @param prompt The user input.
   * @return A configured {@link OrasakaChatRequest}.
   */
  public static OrasakaChatRequest simple(String prompt) {
    return new OrasakaChatRequest(prompt, List.of(), null, null);
  }
}
