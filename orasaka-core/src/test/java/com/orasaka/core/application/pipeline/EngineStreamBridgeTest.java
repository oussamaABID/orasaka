package com.orasaka.core.application.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.event.ChatCompletedEvent;
import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.domain.model.chat.InternalChatResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class EngineStreamBridgeTest {

  @Mock private ChatModel chatModel;
  @Mock private EnginePipelineBridge pipelineBridge;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private OrchestrationPipeline orchestrationPipeline;
  @Mock private PromptContextInterceptor interceptor;
  @Mock private com.orasaka.core.domain.ports.outbound.UserCredentialsProvider credentialsProvider;
  @Mock private com.orasaka.core.application.service.DynamicChatModelFactory modelFactory;

  private EngineStreamBridge streamBridge;

  @BeforeEach
  void setUp() {
    streamBridge =
        new EngineStreamBridge(chatModel, pipelineBridge, credentialsProvider, modelFactory);
  }

  @Test
  void shouldStreamChatResponsesSuccessfully() {
    // Arrange
    var request = InternalChatRequest.simple("hello");
    var prompt = new Prompt("hello", OllamaChatOptions.builder().build());
    var context = new EnginePipelineContext("ollama", "conv-1", "hello", prompt);

    when(pipelineBridge.compileContext(
            request, orchestrationPipeline, "ollama", List.of(interceptor)))
        .thenReturn(context);

    var mockOut1 = mock(org.springframework.ai.chat.messages.AssistantMessage.class);
    when(mockOut1.getText()).thenReturn("chunk 1");
    var gen1 = new Generation(mockOut1, ChatGenerationMetadata.NULL);
    var response1 = new ChatResponse(List.of(gen1));

    var mockOut2 = mock(org.springframework.ai.chat.messages.AssistantMessage.class);
    when(mockOut2.getText()).thenReturn("chunk 2");
    var gen2 = new Generation(mockOut2, ChatGenerationMetadata.NULL);
    var response2 = new ChatResponse(List.of(gen2));

    when(chatModel.stream(prompt)).thenReturn(Flux.just(response1, response2));

    // Act
    Flux<InternalChatResponse> resultFlux =
        streamBridge.createStream(
            request, orchestrationPipeline, "ollama", List.of(interceptor), eventPublisher);

    List<InternalChatResponse> results = resultFlux.collectList().block();

    // Assert
    assertThat(results).isNotNull().hasSize(2);
    assertThat(results.get(0).content()).isEqualTo("chunk 1");
    assertThat(results.get(0).conversationId()).isEqualTo("conv-1");
    assertThat(results.get(0).metadata()).containsEntry("provider", "ollama");

    assertThat(results.get(1).content()).isEqualTo("chunk 2");
    assertThat(results.get(1).conversationId()).isEqualTo("conv-1");
    assertThat(results.get(1).metadata()).containsEntry("provider", "ollama");

    verify(eventPublisher, times(1)).publishEvent(any(ChatCompletedEvent.class));
    verify(interceptor, times(1)).postProcess(request, "hello", "chunk 1chunk 2");
  }

  @Test
  void shouldFallbackToNonToolStreamOnSynchronousException() {
    // Arrange
    var request = InternalChatRequest.simple("hello");
    var prompt = new Prompt("hello", OllamaChatOptions.builder().build());
    var context = new EnginePipelineContext("ollama", "conv-1", "hello", prompt);

    when(pipelineBridge.compileContext(
            request, orchestrationPipeline, "ollama", List.of(interceptor)))
        .thenReturn(context);

    // Sync throw of IllegalStateException
    when(chatModel.stream(prompt)).thenThrow(new IllegalStateException("Tool activation failed"));

    var mockOutFallback = mock(org.springframework.ai.chat.messages.AssistantMessage.class);
    when(mockOutFallback.getText()).thenReturn("fallback chunk");
    var gen = new Generation(mockOutFallback, ChatGenerationMetadata.NULL);
    var responseFallback = new ChatResponse(List.of(gen));

    // Fallback stream when input is not prompt
    when(chatModel.stream((Prompt) argThat(p -> p != prompt)))
        .thenReturn(Flux.just(responseFallback));

    // Act
    Flux<InternalChatResponse> resultFlux =
        streamBridge.createStream(
            request, orchestrationPipeline, "ollama", List.of(interceptor), eventPublisher);

    List<InternalChatResponse> results = resultFlux.collectList().block();

    // Assert
    assertThat(results).isNotNull().hasSize(1);
    assertThat(results.get(0).content()).isEqualTo("fallback chunk");

    verify(eventPublisher, times(1)).publishEvent(any(ChatCompletedEvent.class));
  }

  @Test
  void shouldFallbackToNonToolStreamOnAsynchronousException() {
    // Arrange
    var request = InternalChatRequest.simple("hello");
    var prompt = new Prompt("hello", OllamaChatOptions.builder().build());
    var context = new EnginePipelineContext("ollama", "conv-1", "hello", prompt);

    when(pipelineBridge.compileContext(
            request, orchestrationPipeline, "ollama", List.of(interceptor)))
        .thenReturn(context);

    // Async emission of IllegalStateException
    when(chatModel.stream(prompt))
        .thenReturn(Flux.error(new IllegalStateException("Tool activation async failed")));

    var mockOutFallback = mock(org.springframework.ai.chat.messages.AssistantMessage.class);
    when(mockOutFallback.getText()).thenReturn("fallback chunk async");
    var gen = new Generation(mockOutFallback, ChatGenerationMetadata.NULL);
    var responseFallback = new ChatResponse(List.of(gen));

    // Fallback stream when input is not prompt
    when(chatModel.stream((Prompt) argThat(p -> p != prompt)))
        .thenReturn(Flux.just(responseFallback));

    // Act
    Flux<InternalChatResponse> resultFlux =
        streamBridge.createStream(
            request, orchestrationPipeline, "ollama", List.of(interceptor), eventPublisher);

    List<InternalChatResponse> results = resultFlux.collectList().block();

    // Assert
    assertThat(results).isNotNull().hasSize(1);
    assertThat(results.get(0).content()).isEqualTo("fallback chunk async");

    verify(eventPublisher, times(1)).publishEvent(any(ChatCompletedEvent.class));
  }
}
