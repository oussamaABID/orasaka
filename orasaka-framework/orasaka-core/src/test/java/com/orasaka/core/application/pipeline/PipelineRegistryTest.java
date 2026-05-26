package com.orasaka.core.application.pipeline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.model.PipelineConfig;
import com.orasaka.core.domain.ports.outbound.PipelineConfigProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PipelineRegistryTest {

  @Mock private PipelineConfigProvider configProvider;

  private PipelineRegistry registry;

  @BeforeEach
  void setUp() {
    // Return empty list by default to let initial load run quietly
    when(configProvider.findAllOrdered()).thenReturn(List.of());
    registry = new PipelineRegistry(configProvider);
  }

  @Test
  void getConfig_nonExistentPipelineId_returnsDefaultConfig() {
    PipelineConfig config = registry.getConfig("non-existent");
    assertNotNull(config);
    assertEquals(PipelineConfig.DEFAULT_PIPELINE_ID, config.pipelineId());
    assertFalse(config.coreInterceptorKeys().isEmpty());
    assertTrue(config.dynamicInterceptorKeys().isEmpty());
  }

  @Test
  void getConfig_nullPipelineId_returnsDefaultConfig() {
    PipelineConfig config = registry.getConfig(null);
    assertNotNull(config);
    assertEquals(PipelineConfig.DEFAULT_PIPELINE_ID, config.pipelineId());
  }

  @Test
  void getActiveInterceptorIds_returnsCoreAndDynamicInterceptors() {
    InterceptorConfig coreConfig =
        new InterceptorConfig(
            "UserContextResolver", "User Context Resolver", 0, true, "Core interceptor");
    InterceptorConfig dynamicConfig =
        new InterceptorConfig(
            "RefinerInterceptor", "Refiner Interceptor", 3, true, "Dynamic interceptor");

    when(configProvider.findAllOrdered()).thenReturn(List.of(coreConfig, dynamicConfig));
    registry.reload();

    List<String> activeIds = registry.getActiveInterceptorIds(PipelineConfig.DEFAULT_PIPELINE_ID);
    assertNotNull(activeIds);
    assertTrue(activeIds.contains("UserContextResolver"));
    assertTrue(activeIds.contains("RefinerInterceptor"));
  }

  @Test
  void reload_providerThrowsException_retainsPreviousSnapshot() {
    InterceptorConfig coreConfig =
        new InterceptorConfig(
            "UserContextResolver", "User Context Resolver", 0, true, "Core interceptor");
    when(configProvider.findAllOrdered()).thenReturn(List.of(coreConfig));
    registry.reload();

    PipelineConfig originalConfig = registry.getConfig(PipelineConfig.DEFAULT_PIPELINE_ID);

    // Make provider throw exception on next reload
    when(configProvider.findAllOrdered()).thenThrow(new RuntimeException("DB error"));
    registry.reload();

    PipelineConfig postErrorConfig = registry.getConfig(PipelineConfig.DEFAULT_PIPELINE_ID);
    assertEquals(originalConfig, postErrorConfig);
  }

  @Test
  void reload_providerThrowsExceptionOnEmptyCache_loadsDefaultConfig() {
    PipelineConfigProvider failingProvider = mock(PipelineConfigProvider.class);
    when(failingProvider.findAllOrdered()).thenThrow(new RuntimeException("DB error"));

    PipelineRegistry registryWithError = new PipelineRegistry(failingProvider);
    PipelineConfig config = registryWithError.getConfig(PipelineConfig.DEFAULT_PIPELINE_ID);
    assertNotNull(config);
    assertEquals(PipelineConfig.DEFAULT_PIPELINE_ID, config.pipelineId());
  }
}
