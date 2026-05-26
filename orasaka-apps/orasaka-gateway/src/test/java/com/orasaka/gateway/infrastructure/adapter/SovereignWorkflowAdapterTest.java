package com.orasaka.gateway.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.business.domain.model.SovereignWorkflowContext;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
  @DisplayName("Maps SovereignWorkflowContext into Core ChatRequest with namespaced preferences")
  void executeSovereignPrompt_mapsContextAndCallsClient() {
    String userPrompt = "Check compliance";
    SovereignWorkflowContext workflowContext =
        new SovereignWorkflowContext(
            "session-123",
            "Mask PII data",
            "PRO",
            Set.of("RefinerInterceptor"),
            Set.of("MemoryInterceptor"),
            Map.of("orgId", "krizaka"));

    ChatResponse response = new ChatResponse("Masked Output", "session-123", Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(response);

    String result = adapter.executeSovereignPrompt(userPrompt, workflowContext);

    assertEquals("Masked Output", result);

    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(captor.capture());

    ChatRequest capturedRequest = captor.getValue();
    assertEquals(userPrompt, capturedRequest.prompt());
    assertEquals(1, capturedRequest.messages().size());
    assertEquals("system", capturedRequest.messages().get(0).role());
    assertEquals("Mask PII data", capturedRequest.messages().get(0).content());
    assertEquals("session-123", capturedRequest.context().conversationId());

    // Verify namespaced preference mapping
    Map<String, Object> prefs = capturedRequest.context().preferences();
    assertEquals("session-123", prefs.get("orasaka.pipeline.contextId"));
    assertEquals("PRO", prefs.get("orasaka.user.tier"));
    assertTrue(
        ((Set<?>) prefs.get("orasaka.pipeline.forcedInterceptors")).contains("RefinerInterceptor"));
    assertTrue(
        ((Set<?>) prefs.get("orasaka.pipeline.skippedInterceptors")).contains("MemoryInterceptor"));
    assertEquals("krizaka", prefs.get("orasaka.user.meta.orgId"));
  }

  @Test
  @DisplayName("Returns empty string when AiClient returns null response")
  void executeSovereignPrompt_returnsEmptyOnNullResponse() {
    SovereignWorkflowContext workflowContext =
        SovereignWorkflowContext.minimal("ctx-1", "instructions");
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(null);

    String result = adapter.executeSovereignPrompt("query", workflowContext);

    assertEquals("", result);
  }
}
