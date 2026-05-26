package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Default fallback strategy for text/chat generation jobs.
 *
 * <p>This strategy handles any feature key that doesn't match a more specific strategy. It has the
 * lowest priority in the strategy chain.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
final class ChatGenerationStrategy implements JobExecutionStrategy {

  private final AiClient aiClient;

  ChatGenerationStrategy(AiClient aiClient) {
    this.aiClient = aiClient;
  }

  @Override
  public boolean supports(String featureKey) {
    // Catch-all — this is the default strategy
    return true;
  }

  @Override
  public Map<String, Object> execute(JobMessage message, Context context)
      throws JobExecutionException {
    String prompt = (String) message.payload().get("prompt");
    if (prompt == null) {
      prompt = (String) message.payload().get("text");
    }
    if (prompt == null) {
      throw new IllegalArgumentException("Payload does not contain prompt or text field");
    }

    ChatRequest chatRequest = new ChatRequest(prompt, null, null, context);
    ChatResponse chatResponse = aiClient.chat(chatRequest);

    Map<String, Object> result = new HashMap<>();
    result.put("content", chatResponse.content());
    if (chatResponse.metadata() != null) {
      result.put("metadata", chatResponse.metadata());
    }
    return result;
  }
}
