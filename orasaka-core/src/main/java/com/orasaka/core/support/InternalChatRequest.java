package com.orasaka.core.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

public record InternalChatRequest(
    String prompt, List<ChatMessage> messages, OrasakaOptions options, OrasakaContext context) {

  public InternalChatRequest {
    messages = (messages == null) ? List.of() : List.copyOf(messages);
  }

  public List<Message> compileMessages(
      String refinedPrompt, Function<ChatMessage, Message> messageMapper) {
    List<Message> container = new ArrayList<>();
    if (!this.messages.isEmpty()) {
      container.addAll(this.messages.stream().map(messageMapper).toList());
    }
    if (refinedPrompt != null && !refinedPrompt.isBlank()) {
      container.add(new UserMessage(refinedPrompt));
    }
    return new ArrayList<>(container);
  }

  public record ChatMessage(String role, String content) {
    public ChatMessage {
      if (role == null || role.isBlank()) {
        throw new IllegalArgumentException("Role cannot be empty");
      }
      if (content == null) {
        content = "";
      }
    }
  }

  public static InternalChatRequest simple(String prompt) {
    return new InternalChatRequest(prompt, List.of(), null, null);
  }
}
