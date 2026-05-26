package com.orasaka.gateway.application.service;

import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.ports.outbound.PipelineConfigProvider;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PipelineInterceptorConfigEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.PipelineInterceptorConfigRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gateway-layer implementation of the pipeline configuration outbound port. Bridges core domain
 * records with persistence entities following the FeatureToggleProvider pattern.
 */
@Service
class PipelineConfigProviderImpl implements PipelineConfigProvider {

  private final PipelineInterceptorConfigRepository repository;

  PipelineConfigProviderImpl(PipelineInterceptorConfigRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "PipelineInterceptorConfigRepository must not be null");
  }

  @Override
  @Transactional(readOnly = true)
  public List<InterceptorConfig> findAllOrdered() {
    return repository.findAllByOrderByExecutionOrderAsc().stream()
        .map(PipelineInterceptorConfigMapper::toRecord)
        .toList();
  }

  @Override
  @Transactional
  public InterceptorConfig save(InterceptorConfig config) {
    PipelineInterceptorConfigEntity entity =
        repository
            .findByInterceptorKey(config.interceptorKey())
            .map(
                existing -> {
                  existing.setDisplayLabel(config.displayLabel());
                  existing.setExecutionOrder(config.executionOrder());
                  existing.setIsEnabled(config.enabled());
                  existing.setDescription(config.description());
                  return existing;
                })
            .orElseGet(() -> PipelineInterceptorConfigMapper.toEntity(config));
    return PipelineInterceptorConfigMapper.toRecord(repository.save(entity));
  }

  @Override
  @Transactional
  public List<InterceptorConfig> saveAll(List<InterceptorConfig> configs) {
    return configs.stream().map(this::save).toList();
  }

  @Override
  @Transactional
  public void resetToDefaults(List<InterceptorConfig> defaults) {
    repository.deleteAll();
    List<PipelineInterceptorConfigEntity> entities =
        defaults.stream().map(PipelineInterceptorConfigMapper::toEntity).toList();
    repository.saveAll(entities);
  }
}
