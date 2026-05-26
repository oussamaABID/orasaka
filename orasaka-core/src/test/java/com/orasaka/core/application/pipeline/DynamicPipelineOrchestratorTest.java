package com.orasaka.core.application.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.model.Authority;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.domain.model.RoutingMode;
import com.orasaka.core.domain.ports.outbound.PipelineConfigProvider;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.core.infrastructure.config.SecurityProperties;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamicPipelineOrchestrator")
class DynamicPipelineOrchestratorTest {

  @Mock private PipelineConfigProvider configProvider;
  @Mock private CoreProperties properties;
  @Mock private CoreProperties.OrchestrationConfig orchestrationConfig;

  private static final Context TEST_CONTEXT =
      new Context("user-1", "conv-1", Map.of("locale", "fr"), Set.of(new Authority("ROLE_USER")));

  @Nested
  @DisplayName("Security Kill-Switch")
  class SecurityKillSwitchTests {

    @Test
    @DisplayName("throws SecurityException for AI-dependent interceptor when kill-switch is active")
    void throwsSecurityExceptionForAiDependentInterceptor() {
      var aiInterceptor = new StubAiDependentInterceptor();
      SecurityProperties secProps = new SecurityProperties(true);

      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);
      when(orchestrationConfig.routing()).thenReturn(null);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(aiInterceptor), properties, secProps, configProvider);

      when(configProvider.findAllOrdered()).thenReturn(List.of());

      assertThatThrownBy(() -> orchestrator.process("test query", TEST_CONTEXT))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("AI governance kill-switch is active")
          .hasMessageContaining("StubAiDependentInterceptor");
    }

    @Test
    @DisplayName("executes AI-dependent interceptor normally when kill-switch is disabled")
    void executesAiDependentInterceptorWhenKillSwitchDisabled() {
      var aiInterceptor = new StubAiDependentInterceptor();
      SecurityProperties secProps = new SecurityProperties(false);

      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);
      when(orchestrationConfig.routing()).thenReturn(null);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(aiInterceptor), properties, secProps, configProvider);

      when(configProvider.findAllOrdered()).thenReturn(List.of());

      PromptContext result = orchestrator.process("test query", TEST_CONTEXT);
      assertThat(result).isNotNull();
      assertThat(result.refinedPrompt()).isEqualTo("[refined] test query");
    }

    @Test
    @DisplayName("executes non-AI-dependent interceptors normally even when kill-switch is active")
    void executesNonAiDependentInterceptorWithKillSwitchActive() {
      var safeInterceptor = new StubNonAiInterceptor();
      SecurityProperties secProps = new SecurityProperties(true);

      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);
      when(orchestrationConfig.routing()).thenReturn(null);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(safeInterceptor), properties, secProps, configProvider);

      when(configProvider.findAllOrdered()).thenReturn(List.of());

      PromptContext result = orchestrator.process("test query", TEST_CONTEXT);
      assertThat(result).isNotNull();
      assertThat(result.systemMetadata()).containsKey("safeInterceptorRan");
    }
  }

  @Nested
  @DisplayName("Pipeline Lifecycle")
  class PipelineLifecycleTests {

    @Test
    @DisplayName("returns null when pipeline is disabled")
    void returnsNullWhenPipelineDisabled() {
      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(false);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(), properties, new SecurityProperties(), configProvider);

      PromptContext result = orchestrator.process("test", TEST_CONTEXT);
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("uses fallback chain when database config is empty")
    void usesFallbackChainWhenDbEmpty() {
      var interceptor = new StubNonAiInterceptor();
      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);
      when(orchestrationConfig.routing()).thenReturn(null);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(interceptor), properties, new SecurityProperties(), configProvider);

      when(configProvider.findAllOrdered()).thenReturn(List.of());

      PromptContext result = orchestrator.process("test query", TEST_CONTEXT);
      assertThat(result).isNotNull();
      assertThat(result.systemMetadata()).containsKey("safeInterceptorRan");
    }

    @Test
    @DisplayName("uses database-driven chain when configs are available")
    void usesDatabaseDrivenChain() {
      var interceptorA = new StubNonAiInterceptor();
      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);
      when(orchestrationConfig.routing()).thenReturn(null);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(interceptorA), properties, new SecurityProperties(), configProvider);

      var dbConfig =
          new InterceptorConfig("StubNonAiInterceptor", "Stub Safe", 1, true, "test stub");
      when(configProvider.findAllOrdered()).thenReturn(List.of(dbConfig));

      PromptContext result = orchestrator.process("test query", TEST_CONTEXT);
      assertThat(result).isNotNull();
      assertThat(result.systemMetadata()).containsKey("safeInterceptorRan");
    }

    @Test
    @DisplayName("skips disabled interceptors from database config")
    void skipsDisabledInterceptors() {
      var interceptor = new StubNonAiInterceptor();
      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);
      when(orchestrationConfig.routing()).thenReturn(null);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(interceptor), properties, new SecurityProperties(), configProvider);

      var dbConfig =
          new InterceptorConfig("StubNonAiInterceptor", "Stub Safe", 1, false, "disabled");
      when(configProvider.findAllOrdered()).thenReturn(List.of(dbConfig));

      PromptContext result = orchestrator.process("test query", TEST_CONTEXT);
      assertThat(result).isNotNull();
      assertThat(result.systemMetadata()).doesNotContainKey("safeInterceptorRan");
    }
  }

  @Nested
  @DisplayName("Routing Modes")
  class RoutingModeTests {

    @Test
    @DisplayName("defaults to DETERMINISTIC routing when no mode is configured")
    void defaultsToDeterministicRouting() {
      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);
      when(orchestrationConfig.routing()).thenReturn(null);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(new StubNonAiInterceptor()),
              properties,
              new SecurityProperties(),
              configProvider);

      when(configProvider.findAllOrdered()).thenReturn(List.of());

      PromptContext result = orchestrator.process("test", TEST_CONTEXT);
      assertThat(result).isNotNull();
      assertThat(result.routingMode()).isEqualTo(RoutingMode.DETERMINISTIC);
    }

    @Test
    @DisplayName("sets AGENTIC routing mode when configured")
    void setsAgenticRoutingMode() {
      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);

      var routingConfig = new CoreProperties.RoutingConfig(RoutingMode.AGENTIC);
      when(orchestrationConfig.routing()).thenReturn(routingConfig);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(new StubNonAiInterceptor()),
              properties,
              new SecurityProperties(),
              configProvider);

      when(configProvider.findAllOrdered()).thenReturn(List.of());

      PromptContext result = orchestrator.process("test", TEST_CONTEXT);
      assertThat(result).isNotNull();
      assertThat(result.routingMode()).isEqualTo(RoutingMode.AGENTIC);
    }
  }

  @Nested
  @DisplayName("Cache Management")
  class CacheTests {

    @Test
    @DisplayName("evictChainCache forces rebuild from database on next call")
    void evictChainCacheForcesRebuild() {
      var interceptor = new StubNonAiInterceptor();
      when(properties.orchestration()).thenReturn(orchestrationConfig);
      when(orchestrationConfig.enabled()).thenReturn(true);
      when(orchestrationConfig.routing()).thenReturn(null);

      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(interceptor), properties, new SecurityProperties(), configProvider);

      // First call populates cache
      when(configProvider.findAllOrdered()).thenReturn(List.of());
      orchestrator.process("first call", TEST_CONTEXT);

      // Evict cache
      orchestrator.evictChainCache();

      // Second call should re-query database
      when(configProvider.findAllOrdered()).thenReturn(List.of());
      PromptContext result = orchestrator.process("second call", TEST_CONTEXT);
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("Default Configs")
  class DefaultConfigTests {

    @Test
    @DisplayName("buildDefaultConfigs returns fallback chain as InterceptorConfig records")
    void buildDefaultConfigsReturnsFallbackChain() {
      var interceptor = new StubNonAiInterceptor();
      var orchestrator =
          new DynamicPipelineOrchestrator(
              List.of(interceptor), properties, new SecurityProperties(), configProvider);

      List<InterceptorConfig> defaults = orchestrator.buildDefaultConfigs();
      assertThat(defaults).hasSize(1);
      assertThat(defaults.get(0).interceptorKey()).isEqualTo("StubNonAiInterceptor");
      assertThat(defaults.get(0).enabled()).isTrue();
    }
  }

  // ─── Stub Interceptors ──────────────────────────────────────────────────────

  /** Stub interceptor that is AI-dependent — will be blocked by kill-switch. */
  static class StubAiDependentInterceptor implements PromptContextInterceptor {
    @Override
    public boolean isAiDependent() {
      return true;
    }

    @Override
    public PromptContext intercept(PromptContext context) {
      return context.withRefinedPrompt("[refined] " + context.rawUserQuery());
    }

    @Override
    public int getOrder() {
      return 1;
    }
  }

  /** Stub interceptor that is NOT AI-dependent — always safe. */
  static class StubNonAiInterceptor implements PromptContextInterceptor {
    @Override
    public PromptContext intercept(PromptContext context) {
      var metadata = new java.util.HashMap<>(context.systemMetadata());
      metadata.put("safeInterceptorRan", true);
      return context.withSystemMetadata(Map.copyOf(metadata));
    }

    @Override
    public int getOrder() {
      return 1;
    }
  }
}
