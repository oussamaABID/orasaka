package com.orasaka.core.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.engine.CoreProperties;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RefinerInterceptor} pipeline stage. */
class RefinerInterceptorTest {

  @Nested
  @DisplayName("Disabled refinement")
  class DisabledRefinement {

    @Test
    @DisplayName("returns context unchanged when orchestration is null")
    void nullOrchestration() {
      var props = new CoreProperties("ollama", Map.of(), null, null, null, null);
      var interceptor = new RefinerInterceptor(Map.of(), props);
      var ctx = new PromptContext("hello", Map.of());

      var result = interceptor.intercept(ctx);
      assertSame(ctx, result);
    }

    @Test
    @DisplayName("returns context unchanged when refiner is disabled")
    void refinerDisabled() {
      var refiner = new CoreProperties.InterceptorConfig(false, "ollama", "llama3", 0.0);
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, refiner, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      var interceptor = new RefinerInterceptor(Map.of(), props);
      var ctx = new PromptContext("hello", Map.of());

      var result = interceptor.intercept(ctx);
      assertSame(ctx, result);
    }
  }

  @Nested
  @DisplayName("Enabled refinement – provider resolution")
  class ProviderResolution {

    @Test
    @DisplayName("returns context unchanged when provider model is not registered")
    void unregisteredProvider() {
      var refiner = new CoreProperties.InterceptorConfig(true, "nonexistent", "m", 0.1);
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, refiner, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      var interceptor = new RefinerInterceptor(Map.of(), props);
      var ctx = new PromptContext("hello", Map.of());

      var result = interceptor.intercept(ctx);
      assertSame(ctx, result);
    }

    @Test
    @DisplayName("uses default provider when refiner provider is null")
    void fallsBackToDefaultProvider() {
      var refiner = new CoreProperties.InterceptorConfig(true, null, "m", 0.1);
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, refiner, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      // No model registered for "ollama" → returns context unchanged
      var interceptor = new RefinerInterceptor(Map.of(), props);
      var ctx = new PromptContext("hello", Map.of());

      var result = interceptor.intercept(ctx);
      assertSame(ctx, result);
    }
  }

  @Nested
  @DisplayName("Constructor validation")
  class ConstructorValidation {

    @Test
    @DisplayName("null chatModels defaults to empty map")
    void nullChatModels() {
      var props = new CoreProperties("ollama", Map.of(), null, null, null, null);
      assertDoesNotThrow(() -> new RefinerInterceptor(null, props));
    }

    @Test
    @DisplayName("null properties throws NPE")
    void nullProperties() {
      assertThrows(NullPointerException.class, () -> new RefinerInterceptor(Map.of(), null));
    }
  }

  @Test
  @DisplayName("getOrder returns 3")
  void orderIs3() {
    var props = new CoreProperties("ollama", Map.of(), null, null, null, null);
    var interceptor = new RefinerInterceptor(Map.of(), props);
    assertEquals(3, interceptor.getOrder());
  }
}
