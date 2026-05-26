package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.Context;
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
class ChatGenerationStrategyTest {

  @Mock private AiClient aiClient;

  private ChatGenerationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new ChatGenerationStrategy(aiClient);
  }

  @Test
  void supports_anyKey_returnsTrue() {
    assertTrue(strategy.supports("orasaka.core.chat.text"));
    assertTrue(strategy.supports("anything.else"));
    assertTrue(strategy.supports(null));
  }

  @Test
  void execute_successfulChatWithPrompt_returnsResponseContent() throws Exception {
    JobMessage message =
        new JobMessage("job-1", "user-1", "chat", Map.of("prompt", "What is Java?"));
    Context context = Context.anonymous();

    ChatResponse response =
        new ChatResponse("Java is a programming language", "conv-1", Map.of("tokens", 100));
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(response);

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertEquals("Java is a programming language", result.get("content"));
    assertEquals(Map.of("tokens", 100), result.get("metadata"));

    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(captor.capture());
    assertEquals("What is Java?", captor.getValue().prompt());
  }

  @Test
  void execute_successfulChatWithText_returnsResponseContent() throws Exception {
    JobMessage message =
        new JobMessage("job-1", "user-1", "chat", Map.of("text", "Tell me a joke"));
    Context context = Context.anonymous();

    ChatResponse response = new ChatResponse("Why did the chicken cross the road?", "conv-1", null);
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(response);

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertEquals("Why did the chicken cross the road?", result.get("content"));
    assertNull(result.get("metadata"));

    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(captor.capture());
    assertEquals("Tell me a joke", captor.getValue().prompt());
  }

  @Test
  void execute_missingPromptAndText_throwsIllegalArgumentException() {
    JobMessage message = new JobMessage("job-1", "user-1", "chat", Map.of());
    Context context = Context.anonymous();

    assertThrows(IllegalArgumentException.class, () -> strategy.execute(message, context));
  }
}
