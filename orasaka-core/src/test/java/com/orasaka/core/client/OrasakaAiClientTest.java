package com.orasaka.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.engine.OrasakaEngine;
import com.orasaka.core.pipeline.OrasakaKnowledgeService;
import com.orasaka.core.pipeline.OrasakaToolRegistry;
import com.orasaka.core.support.InternalChatRequest;
import com.orasaka.core.support.InternalChatResponse;
import com.orasaka.core.support.OrasakaChatRequest;
import com.orasaka.core.support.OrasakaChatResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrasakaAiClientTest {

  @Mock private OrasakaEngine engine;
  @Mock private OrasakaToolRegistry toolRegistry;
  @Mock private OrasakaKnowledgeService knowledgeService;

  private OrasakaAiClient client;

  @BeforeEach
  void setUp() {
    client = new OrasakaAiClient(engine, toolRegistry, knowledgeService);
  }

  @Test
  void shouldDelegateChatToEngine() {
    // Given
    OrasakaChatRequest request = OrasakaChatRequest.simple("test prompt");
    InternalChatResponse engineResponse = new InternalChatResponse("test response", null, Map.of());
    when(engine.chat(any(InternalChatRequest.class))).thenReturn(engineResponse);

    // When
    OrasakaChatResponse actualResponse = client.chat(request);

    // Then
    assertThat(actualResponse.content()).isEqualTo("test response");
    verify(engine).chat(any(InternalChatRequest.class));
  }

  @Test
  void shouldReturnComponents() {
    assertThat(client.getToolRegistry()).isEqualTo(toolRegistry);
    assertThat(client.getKnowledgeService()).isEqualTo(knowledgeService);
  }
}
