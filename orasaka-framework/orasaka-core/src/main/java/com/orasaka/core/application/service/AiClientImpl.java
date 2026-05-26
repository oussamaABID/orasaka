package com.orasaka.core.application.service;

import com.orasaka.core.domain.model.audio.AudioRequest;
import com.orasaka.core.domain.model.audio.AudioResponse;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.model.image.ImageRequest;
import com.orasaka.core.domain.model.image.ImageResponse;
import com.orasaka.core.domain.model.video.VideoRequest;
import com.orasaka.core.domain.model.video.VideoResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.core.domain.ports.outbound.AudioGeneratorClient;
import com.orasaka.core.domain.ports.outbound.ChatGeneratorClient;
import com.orasaka.core.domain.ports.outbound.ImageGeneratorClient;
import com.orasaka.core.domain.ports.outbound.VideoGeneratorClient;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/** Concrete, package-private implementation of {@link AiClient}. */
@Component
class AiClientImpl implements AiClient {

  private static final Logger logger = LoggerFactory.getLogger(AiClientImpl.class);

  private final ChatGeneratorClient chatGeneratorClient;
  private final AudioGeneratorClient audioGeneratorClient;
  private final ImageGeneratorClient imageGeneratorClient;
  private final VideoGeneratorClient videoGeneratorClient;

  public AiClientImpl(
      ChatGeneratorClient chatGeneratorClient,
      AudioGeneratorClient audioGeneratorClient,
      ImageGeneratorClient imageGeneratorClient,
      VideoGeneratorClient videoGeneratorClient) {
    this.chatGeneratorClient =
        Objects.requireNonNull(chatGeneratorClient, "ChatGeneratorClient must not be null");
    this.audioGeneratorClient =
        Objects.requireNonNull(audioGeneratorClient, "AudioGeneratorClient must not be null");
    this.imageGeneratorClient =
        Objects.requireNonNull(imageGeneratorClient, "ImageGeneratorClient must not be null");
    this.videoGeneratorClient =
        Objects.requireNonNull(videoGeneratorClient, "VideoGeneratorClient must not be null");
  }

  @Override
  public ChatResponse chat(ChatRequest request) {
    logger.debug("AiClient chat invoked: {}", request.prompt());
    return chatGeneratorClient.generateChat(request);
  }

  @Override
  public Flux<ChatResponse> stream(ChatRequest request) {
    logger.debug("AiClient stream chat invoked: {}", request.prompt());
    return chatGeneratorClient.streamChat(request);
  }

  @Override
  public AudioResponse audio(AudioRequest request) {
    logger.debug("AiClient audio generation invoked: {}", request.prompt());
    return audioGeneratorClient.generateAudio(request);
  }

  @Override
  public ImageResponse image(ImageRequest request) {
    logger.debug("AiClient image generation invoked: {}", request.prompt());
    return imageGeneratorClient.generateImage(request);
  }

  @Override
  public VideoResponse video(VideoRequest request) {
    logger.debug("AiClient video generation invoked: {}", request.prompt());
    return videoGeneratorClient.generateVideo(request);
  }
}
