package com.orasaka.core.interceptors.memory;

import com.orasaka.core.interceptors.OrasakaContextInterceptor;
import com.orasaka.core.model.OrasakaChatRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

@Component
public class OrasakaMemoryInterceptor implements OrasakaContextInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaMemoryInterceptor.class);

  private final OrasakaMemoryResolver memoryResolver;

  public OrasakaMemoryInterceptor(OrasakaMemoryResolver memoryResolver) {
    this.memoryResolver = memoryResolver;
  }

  @Override
  public ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    String conversationId = (request.context() != null) ? request.context().conversationId() : null;
    if (conversationId != null && !conversationId.isBlank() && memoryResolver != null) {
      ChatMemory chatMemory = memoryResolver.resolve(conversationId);
      List<Message> history = chatMemory.get(conversationId);
      if (history != null && !history.isEmpty()) {
        messages.addAll(history);
        logger.debug(
            "Loaded {} messages from ChatMemory for conversationId: {}",
            history.size(),
            conversationId);
      } else {
        logger.debug("No history found in ChatMemory for conversationId: {}", conversationId);
      }
    }
    return options;
  }

  @Override
  public void postProcess(OrasakaChatRequest request, String promptText, String responseText) {
    String conversationId = (request.context() != null) ? request.context().conversationId() : null;
    if (conversationId != null && !conversationId.isBlank() && memoryResolver != null) {
      ChatMemory chatMemory = memoryResolver.resolve(conversationId);
      List<Message> newMessages = new ArrayList<>();
      if (promptText != null && !promptText.isBlank()) {
        newMessages.add(new UserMessage(promptText));
      }
      newMessages.add(new AssistantMessage(responseText));
      chatMemory.add(conversationId, newMessages);
      logger.debug(
          "Saved new messages (User prompt + Assistant response) to ChatMemory for conversationId: {}",
          conversationId);
    }
  }
}
