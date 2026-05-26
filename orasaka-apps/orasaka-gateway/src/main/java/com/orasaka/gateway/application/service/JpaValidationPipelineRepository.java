package com.orasaka.gateway.application.service;

import com.orasaka.core.domain.model.ValidationPipelineConfiguration;
import com.orasaka.core.domain.ports.outbound.ValidationPipelineRepository;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.ValidationPipelineConfigEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.ValidationPipelineConfigJpaRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gateway-layer implementation of the validation pipeline configuration outbound port. Bridges core
 * domain records with persistence entities following the established {@link
 * PipelineConfigProviderImpl} pattern.
 */
@Service
class JpaValidationPipelineRepository implements ValidationPipelineRepository {

  private final ValidationPipelineConfigJpaRepository jpaRepository;

  JpaValidationPipelineRepository(ValidationPipelineConfigJpaRepository jpaRepository) {
    this.jpaRepository =
        Objects.requireNonNull(
            jpaRepository, "ValidationPipelineConfigJpaRepository must not be null");
  }

  @Override
  @Transactional(readOnly = true)
  public List<ValidationPipelineConfiguration> findAllOrderedByExecution() {
    return jpaRepository.findAllByOrderByExecutionOrderAsc().stream()
        .map(ValidationPipelineConfigMapper::toRecord)
        .toList();
  }

  @Override
  @Transactional
  public ValidationPipelineConfiguration save(ValidationPipelineConfiguration config) {
    ValidationPipelineConfigEntity entity =
        jpaRepository
            .findByStepType(config.stepType().name())
            .map(
                existing -> {
                  existing.setIsEnabled(config.enabled());
                  existing.setExecutionOrder(config.executionOrder());
                  existing.setConfigurationPayload(config.configurationPayload());
                  return existing;
                })
            .orElseGet(() -> ValidationPipelineConfigMapper.toEntity(config));
    return ValidationPipelineConfigMapper.toRecord(jpaRepository.save(entity));
  }

  @Override
  @Transactional
  public List<ValidationPipelineConfiguration> saveAll(
      List<ValidationPipelineConfiguration> configs) {
    return configs.stream().map(this::save).toList();
  }
}
