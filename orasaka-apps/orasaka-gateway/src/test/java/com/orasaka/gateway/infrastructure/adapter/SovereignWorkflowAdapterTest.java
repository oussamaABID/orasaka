package com.orasaka.gateway.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SovereignWorkflowAdapterTest {

  @Mock private AiClient aiClient;

  private SovereignWorkflowAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new SovereignWorkflowAdapter(aiClient);
  }

  @Test
  void executeSovereignPrompt_mapsParametersAndCallsClient() {
    String userPrompt = "Check compliance";
    String systemInstructions = "Mask PII data";
    String contextId = "session-123";

    ChatResponse response = new ChatResponse("Masked Output", contextId, Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(response);

    String result = adapter.executeSovereignPrompt(userPrompt, systemInstructions, contextId);

    assertEquals("Masked Output", result);

    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(captor.capture());

    ChatRequest capturedRequest = captor.getValue();
    assertEquals(userPrompt, capturedRequest.prompt());
    assertEquals(1, capturedRequest.messages().size());
    assertEquals("system", capturedRequest.messages().get(0).role());
    assertEquals(systemInstructions, capturedRequest.messages().get(0).content());
    assertEquals(contextId, capturedRequest.context().conversationId());
  }
}
