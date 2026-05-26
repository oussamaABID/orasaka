package com.orasaka.core.application.pipeline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.model.*;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.core.infrastructure.config.SecurityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamicPipelineExecutorTest {

  @Mock private PipelineRegistry pipelineRegistry;
  @Mock private SemanticRoutingEngine routingEngine;

  private MockInterceptor1 mockInterceptor1;
  private MockInterceptor2 mockInterceptor2;

  private SecurityProperties securityProperties;
  private CoreProperties coreProperties;
  private MeterRegistry meterRegistry;
  private DynamicPipelineExecutor executor;

  private static class MockInterceptor1 implements PromptContextInterceptor {
    private boolean intercepted = false;

    @Override
    public PromptContext beforeExecution(PromptContext context) {
      intercepted = true;
      return context;
    }

    @Override
    public boolean isAiDependent() {
      return false;
    }
  }

  private static class MockInterceptor2 implements PromptContextInterceptor {
    private boolean intercepted = false;

    @Override
    public PromptContext beforeExecution(PromptContext context) {
      intercepted = true;
      return context;
    }

    @Override
    public boolean isAiDependent() {
      return true;
    }
  }

  @BeforeEach
  void setUp() {
    mockInterceptor1 = new MockInterceptor1();
    mockInterceptor2 = new MockInterceptor2();
    securityProperties = new SecurityProperties(false);
    coreProperties =
        new CoreProperties(
            "ollama",
            new CoreProperties.RagConfig(false, "pgvector", 3),
            new CoreProperties.McpConfig(List.of()),
            new CoreProperties.OrchestrationConfig(
                true,
                new CoreProperties.UserContextConfig(true),
                new CoreProperties.SystemContextConfig(true),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.RoutingConfig(
                    RoutingMode.DETERMINISTIC, "http://localhost:8085/v1/classify")),
            null,
            null,
            null,
            null);
    meterRegistry = new SimpleMeterRegistry();
  }

  private void initExecutor(List<PromptContextInterceptor> interceptors) {
    executor =
        new DynamicPipelineExecutor(
            interceptors,
            pipelineRegistry,
            routingEngine,
            securityProperties,
            coreProperties,
            meterRegistry);
  }

  @Nested
  @DisplayName("process()")
  class Process {

    @Test
    @DisplayName("returns null when orchestration is disabled")
    void returnsNullWhenDisabled() {
      coreProperties =
          new CoreProperties(
              "ollama",
              new CoreProperties.RagConfig(false, "pgvector", 3),
              new CoreProperties.McpConfig(List.of()),
              new CoreProperties.OrchestrationConfig(
                  false,
                  new CoreProperties.UserContextConfig(true),
                  new CoreProperties.SystemContextConfig(true),
                  new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                  new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                  new CoreProperties.RoutingConfig(
                      RoutingMode.DETERMINISTIC, "http://localhost:8085/v1/classify")),
              null,
              null,
              null,
              null);
      initExecutor(List.of(mockInterceptor1));

      var context = Context.anonymous();
      var result = executor.process("test", context);

      assertNull(result);
    }

    @Test
    @DisplayName("executes core and dynamic chains successfully in deterministic mode")
    void executesChainsDeterministic() {
      initExecutor(List.of(mockInterceptor1, mockInterceptor2));

      var coreConfig = new InterceptorConfig("MockInterceptor1", "Mock 1", 1, true, "Desc 1");
      var dynamicConfig = new InterceptorConfig("MockInterceptor2", "Mock 2", 2, true, "Desc 2");
      var config =
          new PipelineConfig("default", List.of(coreConfig), List.of(dynamicConfig), List.of());
      when(pipelineRegistry.getConfig(any())).thenReturn(config);

      var context = Context.anonymous();
      var result = executor.process("hello", context, "default");

      assertNotNull(result);
      assertEquals("hello", result.refinedPrompt());
      assertTrue(mockInterceptor1.intercepted);
      assertTrue(mockInterceptor2.intercepted);
    }

    @Test
    @DisplayName("executes core and semantic chains in semantic mode")
    void executesChainsSemantic() {
      coreProperties =
          new CoreProperties(
              "ollama",
              new CoreProperties.RagConfig(false, "pgvector", 3),
              new CoreProperties.McpConfig(List.of()),
              new CoreProperties.OrchestrationConfig(
                  true,
                  new CoreProperties.UserContextConfig(true),
                  new CoreProperties.SystemContextConfig(true),
                  new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                  new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                  new CoreProperties.RoutingConfig(
                      RoutingMode.SEMANTIC, "http://localhost:8085/v1/classify")),
              null,
              null,
              null,
              null);
      initExecutor(List.of(mockInterceptor1, mockInterceptor2));

      var coreConfig = new InterceptorConfig("MockInterceptor1", "Mock 1", 1, true, "Desc 1");
      var config = new PipelineConfig("default", List.of(coreConfig), List.of(), List.of());
      when(pipelineRegistry.getConfig(any())).thenReturn(config);
      when(routingEngine.resolveInterceptors(anyString(), any(), anyMap()))
          .thenReturn(List.of(mockInterceptor2));

      var context = Context.anonymous();
      var result = executor.process("hello", context, "default");

      assertNotNull(result);
      assertTrue(mockInterceptor1.intercepted);
      assertTrue(mockInterceptor2.intercepted);
    }

    @Test
    @DisplayName(
        "throws SecurityException when AI-dependent interceptor run while disableAi is true")
    void throwsSecurityExceptionWhenAiDisabled() {
      // Enable security kill-switch
      securityProperties = new SecurityProperties(true);
      initExecutor(List.of(mockInterceptor1, mockInterceptor2));

      var coreConfig = new InterceptorConfig("MockInterceptor1", "Mock 1", 1, true, "Desc 1");
      var dynamicConfig = new InterceptorConfig("MockInterceptor2", "Mock 2", 2, true, "Desc 2");
      var config =
          new PipelineConfig("default", List.of(coreConfig), List.of(dynamicConfig), List.of());
      when(pipelineRegistry.getConfig(any())).thenReturn(config);

      var context = Context.anonymous();
      assertThrows(SecurityException.class, () -> executor.process("hello", context, "default"));
    }
  }

  @Nested
  @DisplayName("buildSchema()")
  class BuildSchema {

    @Test
    @DisplayName("correctly builds AdvancedPipelineSchema in deterministic mode")
    void buildsSchemaDeterministic() {
      initExecutor(List.of(mockInterceptor1, mockInterceptor2));

      var coreConfig = new InterceptorConfig("MockInterceptor1", "Mock 1", 1, true, "Desc 1");
      var dynamicConfig = new InterceptorConfig("MockInterceptor2", "Mock 2", 2, true, "Desc 2");
      var config =
          new PipelineConfig("default", List.of(coreConfig), List.of(dynamicConfig), List.of());
      when(pipelineRegistry.getConfig(any())).thenReturn(config);

      var schema = executor.buildSchema("default", "test query");

      assertEquals("default", schema.pipelineId());
      assertEquals(List.of("MockInterceptor1"), schema.coreInterceptorIds());
      assertEquals(List.of("MockInterceptor2"), schema.dynamicInterceptorIds());
      assertEquals(10L, schema.estimatedLatencyMs());
    }

    @Test
    @DisplayName("correctly builds AdvancedPipelineSchema in semantic mode")
    void buildsSchemaSemantic() {
      coreProperties =
          new CoreProperties(
              "ollama",
              new CoreProperties.RagConfig(false, "pgvector", 3),
              new CoreProperties.McpConfig(List.of()),
              new CoreProperties.OrchestrationConfig(
                  true,
                  new CoreProperties.UserContextConfig(true),
                  new CoreProperties.SystemContextConfig(true),
                  new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                  new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                  new CoreProperties.RoutingConfig(
                      RoutingMode.SEMANTIC, "http://localhost:8085/v1/classify")),
              null,
              null,
              null,
              null);
      initExecutor(List.of(mockInterceptor1, mockInterceptor2));

      var coreConfig = new InterceptorConfig("MockInterceptor1", "Mock 1", 1, true, "Desc 1");
      var config = new PipelineConfig("default", List.of(coreConfig), List.of(), List.of());
      when(pipelineRegistry.getConfig(any())).thenReturn(config);
      when(routingEngine.resolveInterceptors(anyString(), any(), anyMap()))
          .thenReturn(List.of(mockInterceptor2));

      var schema = executor.buildSchema("default", "test query");

      assertEquals("default", schema.pipelineId());
      assertEquals(List.of("MockInterceptor1"), schema.coreInterceptorIds());
      assertEquals(List.of("MockInterceptor2"), schema.dynamicInterceptorIds());
    }
  }

  @Nested
  @DisplayName("cacheManagement()")
  class CacheManagement {

    @Test
    @DisplayName("evictAndReload and evictChainCache reload pipelineRegistry")
    void evictsAndReloads() {
      initExecutor(List.of());
      executor.evictAndReload();
      verify(pipelineRegistry, times(1)).reload();

      executor.evictChainCache();
      verify(pipelineRegistry, times(2)).reload();
    }
  }

  @Nested
  @DisplayName("configs()")
  class Configs {

    @Test
    @DisplayName("getCurrentConfig returns combined interceptors")
    void returnsCurrentConfig() {
      initExecutor(List.of());
      var coreConfig = new InterceptorConfig("mockInterceptor", "Mock", 1, true, "Desc");
      var config = new PipelineConfig("default", List.of(coreConfig), List.of(), List.of());
      when(pipelineRegistry.getConfig(any())).thenReturn(config);

      var current = executor.getCurrentConfig();
      assertEquals(1, current.size());
      assertEquals("mockInterceptor", current.get(0).interceptorKey());
    }

    @Test
    @DisplayName("buildDefaultConfigs humanizes interceptor names")
    void buildsDefaultConfigs() {
      initExecutor(List.of(mockInterceptor1));
      var defaults = executor.buildDefaultConfigs();

      assertEquals(1, defaults.size());
      assertNotNull(defaults.get(0).displayLabel());
    }
  }
}
