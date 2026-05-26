package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.application.pipeline.DynamicPipelineExecutor;
import com.orasaka.core.domain.model.AdvancedPipelineSchema;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.gateway.domain.model.ChatStreamRequest;
import com.orasaka.gateway.infrastructure.support.SseStreamHelper;
import com.orasaka.identity.domain.model.Persona;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

class ChatStreamControllerTest {

  private AiClient aiClient;
  private UserProfileProvider userProfileProvider;
  private DynamicPipelineExecutor pipelineExecutor;
  private ChatStreamController controller;
  private MockedStatic<SseStreamHelper> mockedSseStreamHelper;
  private SseEmitter mockEmitter;

  @BeforeEach
  void setUp() {
    aiClient = mock(AiClient.class);
    userProfileProvider = mock(UserProfileProvider.class);
    pipelineExecutor = mock(DynamicPipelineExecutor.class);

    when(pipelineExecutor.buildSchema(any(), any()))
        .thenReturn(new AdvancedPipelineSchema("default", List.of(), List.of(), 0L));

    controller = new ChatStreamController(aiClient, userProfileProvider, pipelineExecutor, 30);

    mockEmitter = mock(SseEmitter.class);
    mockedSseStreamHelper = mockStatic(SseStreamHelper.class);
    mockedSseStreamHelper.when(SseStreamHelper::createEmitter).thenReturn(mockEmitter);
  }

  @AfterEach
  void tearDown() {
    mockedSseStreamHelper.close();
  }

  @Test
  void shouldFilterEmptyTokensButPreserveWhitespaceFromStream() throws IOException {
    User mockUser = Persona.freeUser();

    List<ChatResponse> responses =
        List.of(
            new ChatResponse("", "conv-123", Map.of()),
            new ChatResponse("   ", "conv-123", Map.of()),
            new ChatResponse("Hello", "conv-123", Map.of()),
            new ChatResponse("", "conv-123", Map.of()),
            new ChatResponse(" world!", "conv-123", Map.of()),
            new ChatResponse("   ", "conv-123", Map.of()));

    when(aiClient.stream(any(ChatRequest.class))).thenReturn(Flux.fromIterable(responses));

    SseEmitter result = controller.streamChat("conv-123", "prompt", mockUser);

    assertThat(result).isSameAs(mockEmitter);
    verify(mockEmitter, times(5)).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void shouldFilterEmptyAndBlankTokensOnPostStream() throws IOException {
    User mockUser = Persona.freeUser();

    List<ChatResponse> responses =
        List.of(
            new ChatResponse("Hello", "conv-123", Map.of()),
            new ChatResponse("", "conv-123", Map.of()));

    when(aiClient.stream(any(ChatRequest.class))).thenReturn(Flux.fromIterable(responses));

    ChatStreamRequest requestPayload =
        new ChatStreamRequest("prompt", List.of(), null, null, null, null, "default");
    SseEmitter result = controller.streamChatPost("conv-123", requestPayload, mockUser);

    assertThat(result).isSameAs(mockEmitter);
    verify(mockEmitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
  }
}
