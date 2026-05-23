package com.orasaka.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.engine.OrasakaEngine;
import com.orasaka.core.interceptors.rag.OrasakaKnowledgeService;
import com.orasaka.core.interceptors.tool.OrasakaToolRegistry;
import com.orasaka.core.model.OrasakaChatRequest;
import com.orasaka.core.model.OrasakaChatResponse;
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
    OrasakaChatResponse expectedResponse = new OrasakaChatResponse("test response", null, Map.of());
    when(engine.chat(request)).thenReturn(expectedResponse);

    // When
    OrasakaChatResponse actualResponse = client.chat(request);

    // Then
    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(engine).chat(request);
  }

  @Test
  void shouldReturnComponents() {
    assertThat(client.getToolRegistry()).isEqualTo(toolRegistry);
    assertThat(client.getKnowledgeService()).isEqualTo(knowledgeService);
  }
}
