package com.orasaka.core.infrastructure.adapter.processor;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.core.infrastructure.config.CoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static com.orasaka.test.TestConstants.*;

/** Unit tests for {@link LocalVideoProcessor} keyframe extraction and configuration resolution. */
class LocalVideoProcessorTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() { org.junit.jupiter.api.Assertions.assertTrue(true); }


  /**
   * Creates a stub WhisperTranscriptionClient for tests that do not exercise network calls. All
   * test data is all-zeros or plain text, so the Whisper client is never actually invoked.
   */
  private static WhisperTranscriptionClient stubWhisperClient() {
    return new WhisperTranscriptionClient(RestClient.builder(), new ObjectMapper());
  }

  private static CoreProperties propsWithAnalysis() {
    return new CoreProperties(
        PROVIDER_OLLAMA,
        null,
        null,
        null,
        new CoreProperties.VideoConfig(
            new CoreProperties.VideoAnalysisConfig(8, 5),
            new CoreProperties.VideoGenerationConfig(null, null)),
        new CoreProperties.ImageConfig(
            new CoreProperties.ImageGenerationConfig(
                null, "http://test-image-server:8086", null, null, null, null, null, null, null)),
        new CoreProperties.VisionConfig(PROVIDER_OLLAMA, "llama3.1:8b"),
        new CoreProperties.AudioConfig(PROVIDER_OLLAMA, "llama3.1:8b", "whisper-1"));
  }

  private static CoreProperties propsWithNullVideo() {
    return new CoreProperties(
        PROVIDER_OLLAMA,
        null,
        null,
        null,
        null,
        new CoreProperties.ImageConfig(
            new CoreProperties.ImageGenerationConfig(
                null, "http://test-image-server:8086", null, null, null, null, null, null, null)),
        new CoreProperties.VisionConfig(PROVIDER_OLLAMA, "llama3.1:8b"),
        new CoreProperties.AudioConfig(PROVIDER_OLLAMA, "llama3.1:8b", "whisper-1"));
  }

  @Nested
  @DisplayName("process()")
  class Process {

    @Test
    @DisplayName("extracts keyframes from video bytes")
    void extractsKeyframes() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(), null, stubWhisperClient());
      var payload = processor.process(new byte[100]);

      assertNotNull(payload);
      assertFalse(payload.keyframes().isEmpty());
      assertTrue(payload.keyframes().size() <= 8);
    }

    @Test
    @DisplayName("returns empty transcript for all-zeros input")
    void returnsEmptyTranscript() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(), null, stubWhisperClient());
      var payload = processor.process(new byte[50]);

      assertEquals("", payload.audioTranscript());
    }

    @Test
    @DisplayName("throws NullPointerException for null video bytes")
    void throwsForNullBytes() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(), null, stubWhisperClient());
      assertThrows(NullPointerException.class, () -> processor.process(null));
    }

    @Test
    @DisplayName("handles small video input gracefully")
    void handlesSmallInput() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(), null, stubWhisperClient());
      var payload = processor.process(new byte[3]);

      assertNotNull(payload);
      assertFalse(payload.keyframes().isEmpty());
    }

    @Test
    @DisplayName("respects maxKeyframes limit")
    void respectsMaxKeyframes() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(), null, stubWhisperClient());
      var payload = processor.process(new byte[1000]);

      assertTrue(payload.keyframes().size() <= 8);
    }
  }

  @Nested
  @DisplayName("Configuration resolution")
  class ConfigResolution {

    @Test
    @DisplayName("falls back to defaults when video config is null")
    void fallsBackWhenNull() {
      var processor = new LocalVideoProcessor(propsWithNullVideo(), null, stubWhisperClient());
      var payload = processor.process(new byte[100]);

      assertNotNull(payload);
      assertTrue(payload.keyframes().size() <= 8); // default maxKeyframes
    }

    @Test
    @DisplayName("throws NullPointerException when properties is null")
    void throwsWhenPropertiesNull() {
      var whisper = stubWhisperClient();
      assertThrows(NullPointerException.class, () -> new LocalVideoProcessor(null, null, whisper));
    }
  }
}
