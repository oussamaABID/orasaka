package com.orasaka.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.engine.Engine;
import com.orasaka.core.pipeline.KnowledgeService;
import com.orasaka.core.pipeline.ToolRegistry;
import com.orasaka.core.support.ChatRequest;
import com.orasaka.core.support.ChatResponse;
import com.orasaka.core.support.InternalChatRequest;
import com.orasaka.core.support.InternalChatResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link AiClient} facade mapping and delegation to the engine. */
@ExtendWith(MockitoExtension.class)
class AiClientTest {

  @Mock private Engine engine;
  @Mock private ToolRegistry toolRegistry;
  @Mock private KnowledgeService knowledgeService;

  private AiClient client;

  @BeforeEach
  void setUp() {
    client = new AiClient(engine, toolRegistry, knowledgeService);
  }

  @Test
  void shouldDelegateChatToEngine() {
    // Given
    ChatRequest request = ChatRequest.simple("test prompt");
    InternalChatResponse engineResponse = new InternalChatResponse("test response", null, Map.of());
    when(engine.chat(any(InternalChatRequest.class))).thenReturn(engineResponse);

    // When
    ChatResponse actualResponse = client.chat(request);

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
