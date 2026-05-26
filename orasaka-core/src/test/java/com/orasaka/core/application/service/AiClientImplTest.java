package com.orasaka.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.audio.AudioRequest;
import com.orasaka.core.domain.model.audio.AudioResponse;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.model.image.ImageRequest;
import com.orasaka.core.domain.model.image.ImageResponse;
import com.orasaka.core.domain.model.video.VideoRequest;
import com.orasaka.core.domain.model.video.VideoResponse;
import com.orasaka.core.domain.ports.outbound.AudioGeneratorClient;
import com.orasaka.core.domain.ports.outbound.ChatGeneratorClient;
import com.orasaka.core.domain.ports.outbound.ImageGeneratorClient;
import com.orasaka.core.domain.ports.outbound.VideoGeneratorClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class AiClientImplTest {

  @Mock private ChatGeneratorClient chatGeneratorClient;
  @Mock private AudioGeneratorClient audioGeneratorClient;
  @Mock private ImageGeneratorClient imageGeneratorClient;
  @Mock private VideoGeneratorClient videoGeneratorClient;

  private AiClientImpl aiClient;

  @BeforeEach
  void setUp() {
    aiClient =
        new AiClientImpl(
            chatGeneratorClient, audioGeneratorClient, imageGeneratorClient, videoGeneratorClient);
  }

  @Test
  void shouldDelegateChat() {
    ChatRequest request = new ChatRequest("prompt", List.of(), Map.of(), Context.anonymous());
    ChatResponse expectedResponse = new ChatResponse("reply", "conv-1", Map.of());
    when(chatGeneratorClient.generateChat(request)).thenReturn(expectedResponse);

    ChatResponse actualResponse = aiClient.chat(request);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(chatGeneratorClient).generateChat(request);
  }

  @Test
  void shouldDelegateStream() {
    ChatRequest request = new ChatRequest("prompt", List.of(), Map.of(), Context.anonymous());
    ChatResponse chunk = new ChatResponse("chunk", "conv-1", Map.of());
    when(chatGeneratorClient.streamChat(request)).thenReturn(Flux.just(chunk));

    List<ChatResponse> result = aiClient.stream(request).collectList().block();

    assertThat(result).containsExactly(chunk);
    verify(chatGeneratorClient).streamChat(request);
  }

  @Test
  void shouldDelegateAudio() {
    AudioRequest request =
        new AudioRequest("prompt", "alloy", "tts-1", Map.of(), Context.anonymous());
    AudioResponse expectedResponse = new AudioResponse(new byte[] {1, 2, 3}, "mp3");
    when(audioGeneratorClient.generateAudio(request)).thenReturn(expectedResponse);

    AudioResponse actualResponse = aiClient.audio(request);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(audioGeneratorClient).generateAudio(request);
  }

  @Test
  void shouldDelegateImage() {
    ImageRequest request =
        new ImageRequest("prompt", 512, 512, "model", Map.of(), Context.anonymous());
    ImageResponse expectedResponse = new ImageResponse(new byte[] {4, 5}, "url", "png");
    when(imageGeneratorClient.generateImage(request)).thenReturn(expectedResponse);

    ImageResponse actualResponse = aiClient.image(request);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(imageGeneratorClient).generateImage(request);
  }

  @Test
  void shouldDelegateVideo() {
    VideoRequest request = new VideoRequest("prompt", 4, Map.of(), Context.anonymous());
    VideoResponse expectedResponse = new VideoResponse(new byte[] {1, 2}, "mp4");
    when(videoGeneratorClient.generateVideo(request)).thenReturn(expectedResponse);

    VideoResponse actualResponse = aiClient.video(request);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(videoGeneratorClient).generateVideo(request);
  }
}
