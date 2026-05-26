package com.orasaka.gateway.infrastructure.adapter;

import com.orasaka.business.domain.port.SovereignWorkflowOrchestrator;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SovereignWorkflowAdapter implements SovereignWorkflowOrchestrator {

  private final AiClient aiClient;

  public SovereignWorkflowAdapter(AiClient aiClient) {
    this.aiClient = aiClient;
  }

  @Override
  public String executeSovereignPrompt(
      String userPrompt, String systemInstructions, String contextId) {
    List<ChatRequest.ChatMessage> messages =
        List.of(new ChatRequest.ChatMessage("system", systemInstructions));
    Context context =
        new Context("anonymous", contextId != null ? contextId : "none", Map.of(), Set.of());
    ChatRequest infrastructureRequest = new ChatRequest(userPrompt, messages, Map.of(), context);

    ChatResponse response = aiClient.chat(infrastructureRequest);

    return response != null ? response.content() : "";
  }
}
