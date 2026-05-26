package com.orasaka.gateway.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PipelineInterceptorConfigEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.PipelineInterceptorConfigRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PipelineConfigProviderImplTest {

  private final PipelineInterceptorConfigRepository repository =
      mock(PipelineInterceptorConfigRepository.class);
  private final PipelineConfigProviderImpl provider = new PipelineConfigProviderImpl(repository);

  @Test
  void findAllOrdered_returnsMappedConfigs() {
    var entity = new PipelineInterceptorConfigEntity();
    entity.setInterceptorKey("MEMORY");
    entity.setDisplayLabel("Memory");
    entity.setExecutionOrder(5);
    entity.setIsEnabled(true);
    entity.setDescription("Memory interceptor");
    when(repository.findAllByOrderByExecutionOrderAsc()).thenReturn(List.of(entity));
    List<InterceptorConfig> result = provider.findAllOrdered();
    assertEquals(1, result.size());
    assertEquals("MEMORY", result.get(0).interceptorKey());
    assertEquals("Memory", result.get(0).displayLabel());
    assertEquals(5, result.get(0).executionOrder());
    assertTrue(result.get(0).enabled());
  }

  @Test
  void findAllOrdered_emptyList() {
    when(repository.findAllByOrderByExecutionOrderAsc()).thenReturn(List.of());
    assertTrue(provider.findAllOrdered().isEmpty());
  }

  @Test
  void save_newConfig_savesEntity() {
    var config = new InterceptorConfig("ROUTER", "Router", 7, true, "Router interceptor");
    when(repository.findByInterceptorKey("ROUTER")).thenReturn(Optional.empty());
    var savedEntity = new PipelineInterceptorConfigEntity();
    savedEntity.setInterceptorKey("ROUTER");
    savedEntity.setDisplayLabel("Router");
    savedEntity.setExecutionOrder(7);
    savedEntity.setIsEnabled(true);
    savedEntity.setDescription("Router interceptor");
    when(repository.save(any())).thenReturn(savedEntity);
    InterceptorConfig result = provider.save(config);
    assertEquals("ROUTER", result.interceptorKey());
    verify(repository).save(any());
  }

  @Test
  void save_existingConfig_updatesEntity() {
    var existing = new PipelineInterceptorConfigEntity();
    existing.setInterceptorKey("MEMORY");
    existing.setDisplayLabel("Old Label");
    existing.setExecutionOrder(1);
    existing.setIsEnabled(false);
    when(repository.findByInterceptorKey("MEMORY")).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);
    var config = new InterceptorConfig("MEMORY", "New Label", 5, true, "Updated");
    provider.save(config);
    assertEquals("New Label", existing.getDisplayLabel());
    assertEquals(5, existing.getExecutionOrder());
    assertTrue(existing.getIsEnabled());
  }

  @Test
  void constructor_nullRepository_throws() {
    assertThrows(NullPointerException.class, () -> new PipelineConfigProviderImpl(null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void saveAll_savesMultipleConfigs() {
    var config1 = new InterceptorConfig("ROUTER", "Router", 7, true, "Router interceptor");
    var config2 = new InterceptorConfig("MEMORY", "Memory", 5, true, "Memory interceptor");

    when(repository.findByInterceptorKey("ROUTER")).thenReturn(Optional.empty());
    when(repository.findByInterceptorKey("MEMORY")).thenReturn(Optional.empty());

    var saved1 = new PipelineInterceptorConfigEntity();
    saved1.setInterceptorKey("ROUTER");
    saved1.setDisplayLabel("Router");
    saved1.setExecutionOrder(7);
    saved1.setIsEnabled(true);
    saved1.setDescription("Router interceptor");

    var saved2 = new PipelineInterceptorConfigEntity();
    saved2.setInterceptorKey("MEMORY");
    saved2.setDisplayLabel("Memory");
    saved2.setExecutionOrder(5);
    saved2.setIsEnabled(true);
    saved2.setDescription("Memory interceptor");

    when(repository.save(any(PipelineInterceptorConfigEntity.class)))
        .thenReturn(saved1)
        .thenReturn(saved2);

    List<InterceptorConfig> result = provider.saveAll(List.of(config1, config2));

    assertEquals(2, result.size());
    assertEquals("ROUTER", result.get(0).interceptorKey());
    assertEquals("MEMORY", result.get(1).interceptorKey());
  }

  @Test
  void resetToDefaults_deletesAllAndSavesNewDefaults() {
    var config = new InterceptorConfig("ROUTER", "Router", 7, true, "Router interceptor");

    provider.resetToDefaults(List.of(config));

    verify(repository).deleteAll();
    verify(repository).saveAll(anyList());
  }
}
