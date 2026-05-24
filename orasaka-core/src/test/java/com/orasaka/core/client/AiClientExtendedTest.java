package com.orasaka.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.orasaka.core.engine.Engine;
import com.orasaka.core.pipeline.KnowledgeService;
import com.orasaka.core.pipeline.ToolRegistry;
import com.orasaka.core.support.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

/** Extended unit tests for {@link AiClient} — image, speech, streaming, and null-safety. */
@ExtendWith(MockitoExtension.class)
class AiClientExtendedTest {

  @Mock private Engine engine;
  @Mock private ToolRegistry toolRegistry;
  @Mock private KnowledgeService knowledgeService;

  private AiClient client;

  @BeforeEach
  void setUp() {
    client = new AiClient(engine, toolRegistry, knowledgeService);
  }

  @Nested
  @DisplayName("Image generation")
  class ImageGeneration {

    @Test
    @DisplayName("delegates to engine and maps response")
    void delegatesImageGeneration() {
      var engineResp = new InternalImageResponse(new byte[] {1}, "https://img.url", "png");
      when(engine.generateImage(any(InternalImageRequest.class))).thenReturn(engineResp);

      var result = client.generateImage(new ImageRequest("draw cat", 512, 512, null, null));

      assertThat(result.url()).isEqualTo("https://img.url");
      assertThat(result.format()).isEqualTo("png");
      verify(engine).generateImage(any());
    }
  }

  @Nested
  @DisplayName("Speech generation")
  class SpeechGeneration {

    @Test
    @DisplayName("delegates to engine and returns bytes")
    void delegatesSpeechGeneration() {
      var audioBytes = new byte[] {10, 20, 30};
      when(engine.generateSpeech(any(InternalSpeechRequest.class))).thenReturn(audioBytes);

      var result = client.generateSpeech(new SpeechRequest("Hello", null, null));

      assertThat(result).isEqualTo(audioBytes);
      verify(engine).generateSpeech(any());
    }

    @Test
    @DisplayName("handles null speech response")
    void handlesNullResponse() {
      when(engine.generateSpeech(any(InternalSpeechRequest.class))).thenReturn(null);

      var result = client.generateSpeech(new SpeechRequest("Hello", null, null));

      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("Streaming")
  class Streaming {

    @Test
    @DisplayName("maps stream chunks to ChatResponse")
    void mapsStreamChunks() {
      var chunk1 = new InternalChatResponse("Hello", "conv-1", Map.of());
      var chunk2 = new InternalChatResponse(" world", "conv-1", Map.of());
      when(engine.stream(any(InternalChatRequest.class))).thenReturn(Flux.just(chunk1, chunk2));

      var results = client.stream(ChatRequest.simple("test")).collectList().block();

      assertThat(results).hasSize(2);
      assertThat(results.get(0).content()).isEqualTo("Hello");
      assertThat(results.get(1).content()).isEqualTo(" world");
    }
  }

  @Nested
  @DisplayName("Chat with messages and context")
  class ChatWithContext {

    @Test
    @DisplayName("maps chat messages from public to internal format")
    void mapsChatMessages() {
      var msg = new ChatRequest.ChatMessage("user", "hello");
      var request = new ChatRequest("prompt", List.of(msg), null, null);
      var engineResp = new InternalChatResponse("response", null, Map.of());
      when(engine.chat(any(InternalChatRequest.class))).thenReturn(engineResp);

      var result = client.chat(request);

      assertThat(result.content()).isEqualTo("response");
      verify(engine).chat(argThat(r -> r.messages().size() == 1));
    }
  }
}
