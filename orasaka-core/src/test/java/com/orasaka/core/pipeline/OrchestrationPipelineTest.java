package com.orasaka.core.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.engine.CoreProperties;
import com.orasaka.core.support.Authority;
import com.orasaka.core.support.Context;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OrchestrationPipeline} — chain coordination and bypass. */
class OrchestrationPipelineTest {

  @Nested
  @DisplayName("Disabled pipeline")
  class DisabledPipeline {

    @Test
    @DisplayName("returns null when orchestration is null")
    void nullOrchestration() {
      var props = new CoreProperties("ollama", Map.of(), null, null, null, null);
      var pipeline = new OrchestrationPipeline(List.of(), props);

      assertNull(pipeline.process("hello", null));
    }

    @Test
    @DisplayName("returns null when orchestration is disabled")
    void orchestrationDisabled() {
      var orchestration = new CoreProperties.OrchestrationConfig(false, null, null, null, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      var pipeline = new OrchestrationPipeline(List.of(), props);

      assertNull(pipeline.process("hello", null));
    }
  }

  @Nested
  @DisplayName("Enabled pipeline")
  class EnabledPipeline {

    @Test
    @DisplayName("executes interceptor chain in order")
    void executesChain() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      PromptInterceptor appendInterceptor =
          new PromptInterceptor() {
            @Override
            public PromptContext intercept(PromptContext context) {
              return context.withRefinedPrompt(context.refinedPrompt() + " refined");
            }

            @Override
            public int getOrder() {
              return 1;
            }
          };
      var pipeline = new OrchestrationPipeline(List.of(appendInterceptor), props);

      var result = pipeline.process("hello", null);
      assertNotNull(result);
      assertEquals("hello refined", result.refinedPrompt());
    }

    @Test
    @DisplayName("enriches user metadata from Context")
    void enrichesUserMetadata() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      var pipeline = new OrchestrationPipeline(List.of(), props);
      var context =
          new Context("user1", "conv1", Map.of("lang", "fr"), Set.of(new Authority("ADMIN")));

      var result = pipeline.process("hello", context);
      assertNotNull(result);
      assertEquals("user1", result.userMetadata().get("userId"));
      assertEquals("conv1", result.userMetadata().get("conversationId"));
      assertEquals("fr", result.userMetadata().get("lang"));
    }

    @Test
    @DisplayName("handles interceptor exception gracefully")
    void handlesException() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      PromptInterceptor failingInterceptor =
          new PromptInterceptor() {
            @Override
            public PromptContext intercept(PromptContext context) {
              throw new RuntimeException("boom");
            }

            @Override
            public int getOrder() {
              return 1;
            }
          };
      var pipeline = new OrchestrationPipeline(List.of(failingInterceptor), props);

      var result = pipeline.process("hello", null);
      assertNotNull(result);
      assertEquals("hello", result.rawUserQuery());
    }

    @Test
    @DisplayName("sorts interceptors by order")
    void sortsInterceptors() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      PromptInterceptor second =
          new PromptInterceptor() {
            @Override
            public PromptContext intercept(PromptContext context) {
              return context.withRefinedPrompt(context.refinedPrompt() + "-B");
            }

            @Override
            public int getOrder() {
              return 2;
            }
          };
      PromptInterceptor first =
          new PromptInterceptor() {
            @Override
            public PromptContext intercept(PromptContext context) {
              return context.withRefinedPrompt(context.refinedPrompt() + "-A");
            }

            @Override
            public int getOrder() {
              return 1;
            }
          };
      // Pass in wrong order — pipeline should sort
      var pipeline = new OrchestrationPipeline(List.of(second, first), props);

      var result = pipeline.process("X", null);
      assertEquals("X-A-B", result.refinedPrompt());
    }
  }

  @Nested
  @DisplayName("Constructor edge cases")
  class ConstructorEdgeCases {

    @Test
    @DisplayName("null interceptors list treated as empty")
    void nullInterceptors() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", Map.of(), null, null, orchestration, null);
      assertDoesNotThrow(() -> new OrchestrationPipeline(null, props));
    }
  }
}
