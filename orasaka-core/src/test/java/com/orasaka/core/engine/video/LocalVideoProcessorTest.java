package com.orasaka.core.engine.video;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.engine.CoreProperties;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LocalVideoProcessor} keyframe extraction and configuration resolution. */
class LocalVideoProcessorTest {

  private static CoreProperties propsWithAnalysis(int maxKeyframes, int frameInterval) {
    return new CoreProperties(
        "ollama",
        Map.of(),
        null,
        null,
        null,
        new CoreProperties.VideoConfig(
            new CoreProperties.VideoAnalysisConfig(true, maxKeyframes, frameInterval),
            new CoreProperties.VideoGenerationConfig(false, null)));
  }

  private static CoreProperties propsWithNullVideo() {
    return new CoreProperties("ollama", Map.of(), null, null, null, null);
  }

  @Nested
  @DisplayName("process()")
  class Process {

    @Test
    @DisplayName("extracts keyframes from video bytes")
    void extractsKeyframes() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(4, 5));
      var payload = processor.process(new byte[100]);

      assertNotNull(payload);
      assertFalse(payload.keyframes().isEmpty());
      assertTrue(payload.keyframes().size() <= 4);
    }

    @Test
    @DisplayName("returns empty transcript (stub)")
    void returnsEmptyTranscript() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(4, 5));
      var payload = processor.process(new byte[50]);

      assertEquals("", payload.audioTranscript());
    }

    @Test
    @DisplayName("throws NullPointerException for null video bytes")
    void throwsForNullBytes() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(4, 5));
      assertThrows(NullPointerException.class, () -> processor.process(null));
    }

    @Test
    @DisplayName("handles small video input gracefully")
    void handlesSmallInput() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(8, 5));
      var payload = processor.process(new byte[3]);

      assertNotNull(payload);
      assertFalse(payload.keyframes().isEmpty());
    }

    @Test
    @DisplayName("respects maxKeyframes limit")
    void respectsMaxKeyframes() {
      var processor = new LocalVideoProcessor(propsWithAnalysis(2, 1));
      var payload = processor.process(new byte[1000]);

      assertTrue(payload.keyframes().size() <= 2);
    }
  }

  @Nested
  @DisplayName("Configuration resolution")
  class ConfigResolution {

    @Test
    @DisplayName("falls back to defaults when video config is null")
    void fallsBackWhenNull() {
      var processor = new LocalVideoProcessor(propsWithNullVideo());
      var payload = processor.process(new byte[100]);

      assertNotNull(payload);
      assertTrue(payload.keyframes().size() <= 8); // default maxKeyframes
    }

    @Test
    @DisplayName("throws NullPointerException when properties is null")
    void throwsWhenPropertiesNull() {
      assertThrows(NullPointerException.class, () -> new LocalVideoProcessor(null));
    }
  }
}
