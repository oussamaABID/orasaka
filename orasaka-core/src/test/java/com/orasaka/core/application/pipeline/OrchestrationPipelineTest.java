package com.orasaka.core.application.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.model.Authority;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.domain.ports.outbound.PipelineConfigProvider;
import com.orasaka.core.infrastructure.config.CoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OrchestrationPipeline} — chain coordination and bypass. */
class OrchestrationPipelineTest {

  /** Stub provider that returns an empty config list (forces fallback to hardcoded ordering). */
  private static final PipelineConfigProvider EMPTY_PROVIDER =
      new PipelineConfigProvider() {
        @Override
        public List<InterceptorConfig> findAllOrdered() {
          return List.of();
        }

        @Override
        public InterceptorConfig save(InterceptorConfig config) {
          return config;
        }

        @Override
        public List<InterceptorConfig> saveAll(List<InterceptorConfig> configs) {
          return configs;
        }

        @Override
        public void resetToDefaults(List<InterceptorConfig> defaults) {
          // No-op for test: defaults are not persisted in the stub provider
        }
      };

  @Nested
  @DisplayName("Disabled pipeline")
  class DisabledPipeline {

    @Test
    @DisplayName("returns null when orchestration is null")
    void nullOrchestration() {
      var props = new CoreProperties("ollama", null, null, null, null, null, null, null);
      var pipeline = new OrchestrationPipeline(List.of(), props, EMPTY_PROVIDER);

      assertNull(pipeline.process("hello", Context.anonymous()));
    }

    @Test
    @DisplayName("returns null when orchestration is disabled")
    void orchestrationDisabled() {
      var orchestration = new CoreProperties.OrchestrationConfig(false, null, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);
      var pipeline = new OrchestrationPipeline(List.of(), props, EMPTY_PROVIDER);

      assertNull(pipeline.process("hello", Context.anonymous()));
    }
  }

  @Nested
  @DisplayName("Enabled pipeline")
  class EnabledPipeline {

    @Test
    @DisplayName("executes interceptor chain in order")
    void executesChain() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);
      PromptContextInterceptor appendInterceptor =
          new PromptContextInterceptor() {
            @Override
            public PromptContext intercept(PromptContext context) {
              return context.withRefinedPrompt(context.refinedPrompt() + " refined");
            }

            @Override
            public int getOrder() {
              return 1;
            }
          };
      var pipeline = new OrchestrationPipeline(List.of(appendInterceptor), props, EMPTY_PROVIDER);

      var result = pipeline.process("hello", Context.anonymous());
      assertNotNull(result);
      assertEquals("hello refined", result.refinedPrompt());
    }

    @Test
    @DisplayName("enriches user metadata from Context")
    void enrichesUserMetadata() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);
      var pipeline = new OrchestrationPipeline(List.of(), props, EMPTY_PROVIDER);
      var context =
          new Context("user1", "conv1", Map.of("lang", "fr"), Set.of(new Authority("ADMIN")));

      var result = pipeline.process("hello", context);
      assertNotNull(result);
      assertEquals("user1", result.userMetadata().get("userId"));
      assertEquals("conv1", result.userMetadata().get("conversationId"));
      assertEquals("fr", result.userMetadata().get("lang"));
    }

    @Test
    @DisplayName("loads onboarding profile attributes when user-context is enabled")
    void loadsUserProfileAttributes() {
      var userContext = new CoreProperties.UserContextConfig(true);
      var orchestration =
          new CoreProperties.OrchestrationConfig(true, userContext, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);

      var pipeline = new OrchestrationPipeline(List.of(), props, EMPTY_PROVIDER);
      var context =
          new Context(
              "user1",
              "conv1",
              Map.of(
                  "theme", "rose",
                  "voiceModel", "nova",
                  "primaryIndustry", "finance",
                  "aiBehavior", "Concisely answer",
                  "key", "val"),
              null);

      var result = pipeline.process("hello", context);
      assertNotNull(result);
      assertEquals("rose", result.userMetadata().get("theme"));
      assertEquals("nova", result.userMetadata().get("voiceModel"));
      assertEquals("finance", result.userMetadata().get("primaryIndustry"));
      assertEquals("Concisely answer", result.userMetadata().get("aiBehavior"));
      assertEquals("val", result.userMetadata().get("key"));
    }

    @Test
    @DisplayName("handles interceptor exception gracefully")
    void handlesException() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);
      PromptContextInterceptor failingInterceptor =
          new PromptContextInterceptor() {
            @Override
            public PromptContext intercept(PromptContext context) {
              throw new RuntimeException("boom");
            }

            @Override
            public int getOrder() {
              return 1;
            }
          };
      var pipeline = new OrchestrationPipeline(List.of(failingInterceptor), props, EMPTY_PROVIDER);

      var result = pipeline.process("hello", Context.anonymous());
      assertNotNull(result);
      assertEquals("hello", result.rawUserQuery());
    }

    @Test
    @DisplayName("sorts interceptors by order")
    void sortsInterceptors() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);
      PromptContextInterceptor second =
          new PromptContextInterceptor() {
            @Override
            public PromptContext intercept(PromptContext context) {
              return context.withRefinedPrompt(context.refinedPrompt() + "-B");
            }

            @Override
            public int getOrder() {
              return 2;
            }
          };
      PromptContextInterceptor first =
          new PromptContextInterceptor() {
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
      var pipeline = new OrchestrationPipeline(List.of(second, first), props, EMPTY_PROVIDER);

      var result = pipeline.process("X", Context.anonymous());
      assertEquals("X-A-B", result.refinedPrompt());
    }
  }

  @Nested
  @DisplayName("Dynamic ordering")
  class DynamicOrdering {

    /** Named test interceptor — appends "-A". */
    static class AlphaInterceptor implements PromptContextInterceptor {
      @Override
      public PromptContext intercept(PromptContext context) {
        return context.withRefinedPrompt(context.refinedPrompt() + "-A");
      }

      @Override
      public int getOrder() {
        return 1;
      }
    }

    /** Named test interceptor — appends "-B". */
    static class BetaInterceptor implements PromptContextInterceptor {
      @Override
      public PromptContext intercept(PromptContext context) {
        return context.withRefinedPrompt(context.refinedPrompt() + "-B");
      }

      @Override
      public int getOrder() {
        return 2;
      }
    }

    @Test
    @DisplayName("uses DB config when available — reverses order")
    void usesDbConfig() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);

      PromptContextInterceptor alpha = new AlphaInterceptor();
      PromptContextInterceptor beta = new BetaInterceptor();

      // DB config reverses the order: beta first (order 1), alpha second (order 2)
      PipelineConfigProvider reverseProvider =
          new PipelineConfigProvider() {
            @Override
            public List<InterceptorConfig> findAllOrdered() {
              return List.of(
                  new InterceptorConfig("BetaInterceptor", "Beta", 1, true, ""),
                  new InterceptorConfig("AlphaInterceptor", "Alpha", 2, true, ""));
            }

            @Override
            public InterceptorConfig save(InterceptorConfig config) {
              return config;
            }

            @Override
            public List<InterceptorConfig> saveAll(List<InterceptorConfig> configs) {
              return configs;
            }

            @Override
            public void resetToDefaults(List<InterceptorConfig> defaults) {
              // No-op for test: reverse provider does not support reset
            }
          };

      var pipeline = new OrchestrationPipeline(List.of(alpha, beta), props, reverseProvider);

      var result = pipeline.process("X", Context.anonymous());
      // DB says beta first, then alpha → "X-B-A"
      assertEquals("X-B-A", result.refinedPrompt());
    }

    @Test
    @DisplayName("evictChainCache forces rebuild on next call")
    void evictCacheForcesRebuild() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);

      var pipeline = new OrchestrationPipeline(List.of(), props, EMPTY_PROVIDER);
      pipeline.evictChainCache();

      var result = pipeline.process("test", Context.anonymous());
      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Constructor edge cases")
  class ConstructorEdgeCases {

    @Test
    @DisplayName("null interceptors list treated as empty")
    void nullInterceptors() {
      var orchestration = new CoreProperties.OrchestrationConfig(true, null, null, null, null);
      var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);
      assertDoesNotThrow(() -> new OrchestrationPipeline(null, props, EMPTY_PROVIDER));
    }
  }
}
