package com.orasaka.core.infrastructure.video;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.engine.CoreProperties;
import com.orasaka.core.support.CoreException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;

/** Unit tests for {@link VideoService} — constructor validation and disabled-generation guard. */
class VideoServiceTest {

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidation {

    @Test
    @DisplayName("throws CoreException when localai-video base URL is missing")
    void missingBaseUrl() {
      var props = new CoreProperties("ollama", Map.of(), null, null, null, null);
      assertThrows(
          CoreException.class,
          () -> new VideoService(org.springframework.web.client.RestClient.builder(), props));
    }

    @Test
    @DisplayName("throws CoreException when localai-video config has blank base URL")
    void blankBaseUrl() {
      var provider = new CoreProperties.ProviderConfig(null, "  ", null, null, null, null);
      var props =
          new CoreProperties("ollama", Map.of("localai-video", provider), null, null, null, null);
      assertThrows(
          CoreException.class,
          () -> new VideoService(org.springframework.web.client.RestClient.builder(), props));
    }

    @Test
    @DisplayName("succeeds with valid localai-video base URL")
    void validBaseUrl() {
      var provider =
          new CoreProperties.ProviderConfig(null, "http://localhost:8080", null, null, null, null);
      var props =
          new CoreProperties("ollama", Map.of("localai-video", provider), null, null, null, null);
      assertDoesNotThrow(
          () -> new VideoService(org.springframework.web.client.RestClient.builder(), props));
    }
  }

  @Nested
  @DisplayName("Generation guard")
  class GenerationGuard {

    @Test
    @DisplayName("throws NonTransientAiException when generation is disabled")
    void disabledGeneration() {
      var provider =
          new CoreProperties.ProviderConfig(null, "http://localhost:8080", null, null, null, null);
      var props =
          new CoreProperties("ollama", Map.of("localai-video", provider), null, null, null, null);
      var service = new VideoService(org.springframework.web.client.RestClient.builder(), props);

      var request = new com.orasaka.core.ingest.video.VideoRequest("test", 4, null, null);
      assertThrows(NonTransientAiException.class, () -> service.generateVideo(request));
    }

    @Test
    @DisplayName("generation disabled when video config is null")
    void nullVideoConfig() {
      var provider =
          new CoreProperties.ProviderConfig(null, "http://localhost:8080", null, null, null, null);
      var props =
          new CoreProperties("ollama", Map.of("localai-video", provider), null, null, null, null);
      var service = new VideoService(org.springframework.web.client.RestClient.builder(), props);

      var request = new com.orasaka.core.ingest.video.VideoRequest("generate", null, null, null);
      var ex = assertThrows(NonTransientAiException.class, () -> service.generateVideo(request));
      assertTrue(ex.getMessage().contains("disabled"));
    }

    @Test
    @DisplayName("generation disabled when video generation is explicitly disabled")
    void explicitlyDisabled() {
      var provider =
          new CoreProperties.ProviderConfig(null, "http://localhost:8080", null, null, null, null);
      var videoGen = new CoreProperties.VideoGenerationConfig(false, "localai");
      var videoConfig = new CoreProperties.VideoConfig(null, videoGen);
      var props =
          new CoreProperties(
              "ollama", Map.of("localai-video", provider), null, null, null, videoConfig);
      var service = new VideoService(org.springframework.web.client.RestClient.builder(), props);

      var request = new com.orasaka.core.ingest.video.VideoRequest("test", null, null, null);
      assertThrows(NonTransientAiException.class, () -> service.generateVideo(request));
    }
  }
}
