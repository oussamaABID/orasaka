package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CoreProperties} video configuration records and InterceptorConfig
 * unification.
 */
class CorePropertiesVideoConfigTest {

  @Nested
  @DisplayName("VideoConfig records")
  class VideoConfigTests {

    @Test
    @DisplayName("VideoAnalysisConfig stores enabled flag and limits")
    void analysisConfig() {
      var config = new CoreProperties.VideoAnalysisConfig(true, 12, 3);
      assertTrue(config.enabled());
      assertEquals(12, config.maxKeyframes());
      assertEquals(3, config.frameIntervalSec());
    }

    @Test
    @DisplayName("VideoGenerationConfig stores enabled flag and provider")
    void generationConfig() {
      var config = new CoreProperties.VideoGenerationConfig(true, "localai-video");
      assertTrue(config.enabled());
      assertEquals("localai-video", config.provider());
    }

    @Test
    @DisplayName("VideoConfig composes analysis and generation")
    void videoConfigComposition() {
      var analysis = new CoreProperties.VideoAnalysisConfig(true, 8, 5);
      var generation = new CoreProperties.VideoGenerationConfig(false, null);
      var video = new CoreProperties.VideoConfig(analysis, generation);

      assertSame(analysis, video.analysis());
      assertSame(generation, video.generation());
    }

    @Test
    @DisplayName("null video config is accepted by CoreProperties")
    void nullVideoAccepted() {
      var props = new CoreProperties("ollama", Map.of(), null, null, null, null);
      assertNull(props.video());
    }
  }

  @Nested
  @DisplayName("InterceptorConfig unification")
  class InterceptorConfigTests {

    @Test
    @DisplayName("InterceptorConfig works for refiner configuration")
    void refinerConfig() {
      var config = new CoreProperties.InterceptorConfig(true, "ollama", "llama-refine", 0.2);
      assertTrue(config.enabled());
      assertEquals("ollama", config.provider());
      assertEquals("llama-refine", config.model());
      assertEquals(0.2, config.temperature());
    }

    @Test
    @DisplayName("InterceptorConfig works for router configuration")
    void routerConfig() {
      var config = new CoreProperties.InterceptorConfig(true, "openai", "gpt-4", 0.0);
      assertTrue(config.enabled());
      assertEquals("openai", config.provider());
      assertEquals(0.0, config.temperature());
    }

    @Test
    @DisplayName("OrchestrationConfig uses unified InterceptorConfig for both")
    void orchestrationUsesUnifiedConfig() {
      var refiner = new CoreProperties.InterceptorConfig(true, "ollama", "r", 0.3);
      var router = new CoreProperties.InterceptorConfig(false, "openai", "m", 0.0);
      var config =
          new CoreProperties.OrchestrationConfig(
              true,
              new CoreProperties.UserContextConfig(true),
              new CoreProperties.SystemContextConfig(false),
              refiner,
              router);

      assertTrue(config.enabled());
      assertSame(refiner, config.refiner());
      assertSame(router, config.router());
    }
  }
}
