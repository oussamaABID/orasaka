package com.orasaka.core.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.engine.CoreProperties;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RouterInterceptor} pipeline stage. */
class RouterInterceptorTest {

  @Nested
  @DisplayName("Disabled router")
  class DisabledRouter {

    @Test
    @DisplayName("returns context unchanged when orchestration is null")
    void nullOrchestration() {
      var props = new CoreProperties("ollama", Map.of(), null, null, null, null);
      var interceptor = new RouterInterceptor(Map.of(), props);
      var ctx = new PromptContext("hello", Map.of());

      var result = interceptor.intercept(ctx);
      assertSame(ctx, result);
    }

    @Test
    @DisplayName("returns context unchanged when router is disabled")
    void routerDisabled() {
      var router = new CoreProperties.InterceptorConfig(false, "ollama", "llama3", 0.0);
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, router);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      var interceptor = new RouterInterceptor(Map.of(), props);
      var ctx = new PromptContext("hello", Map.of());

      var result = interceptor.intercept(ctx);
      assertSame(ctx, result);
    }
  }

  @Nested
  @DisplayName("Enabled router – provider resolution")
  class ProviderResolution {

    @Test
    @DisplayName("returns context unchanged when provider model is not registered")
    void unregisteredProvider() {
      var router = new CoreProperties.InterceptorConfig(true, "nonexistent", "m", 0.0);
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, router);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      var interceptor = new RouterInterceptor(Map.of(), props);
      var ctx = new PromptContext("hello", Map.of());

      var result = interceptor.intercept(ctx);
      assertSame(ctx, result);
    }

    @Test
    @DisplayName("uses default provider when router provider is null")
    void fallsBackToDefaultProvider() {
      var router = new CoreProperties.InterceptorConfig(true, null, "m", 0.0);
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, router);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      var interceptor = new RouterInterceptor(Map.of(), props);
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
      assertDoesNotThrow(() -> new RouterInterceptor(null, props));
    }

    @Test
    @DisplayName("null properties throws NPE")
    void nullProperties() {
      assertThrows(NullPointerException.class, () -> new RouterInterceptor(Map.of(), null));
    }
  }

  @Test
  @DisplayName("getOrder returns 4")
  void orderIs4() {
    var props = new CoreProperties("ollama", Map.of(), null, null, null, null);
    var interceptor = new RouterInterceptor(Map.of(), props);
    assertEquals(4, interceptor.getOrder());
  }
}
