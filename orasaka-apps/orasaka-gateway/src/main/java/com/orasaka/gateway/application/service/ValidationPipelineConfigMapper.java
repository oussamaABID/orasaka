package com.orasaka.gateway.application.service;

import com.orasaka.core.domain.model.ValidationPipelineConfiguration;
import com.orasaka.core.domain.model.ValidationStepType;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.ValidationPipelineConfigEntity;
import java.util.Map;

/**
 * Package-private static mapper for validation pipeline config entities ↔ domain records.
 *
 * <p>Handles the {@code String ↔ ValidationStepType} conversion at the boundary between the
 * persistence layer (which uses plain strings per §1.2) and the core domain (which uses the {@link
 * ValidationStepType} enum).
 *
 * <p>Follows the established {@link PipelineInterceptorConfigMapper} pattern. All mapping logic is
 * isolated in this utility — no field-by-field mapping in controllers or services [ERR-107].
 */
final class ValidationPipelineConfigMapper {

  private ValidationPipelineConfigMapper() {}

  /**
   * Maps a JPA entity to a domain record.
   *
   * @param entity The JPA entity.
   * @return The domain record.
   */
  static ValidationPipelineConfiguration toRecord(ValidationPipelineConfigEntity entity) {
    Map<String, Object> payload =
        entity.getConfigurationPayload() != null ? entity.getConfigurationPayload() : Map.of();
    return new ValidationPipelineConfiguration(
        entity.getId(),
        ValidationStepType.valueOf(entity.getStepType()),
        Boolean.TRUE.equals(entity.getIsEnabled()),
        entity.getExecutionOrder(),
        payload);
  }

  /**
   * Maps a domain record to a JPA entity.
   *
   * @param config The domain record.
   * @return A new JPA entity populated from the record.
   */
  static ValidationPipelineConfigEntity toEntity(ValidationPipelineConfiguration config) {
    var entity = new ValidationPipelineConfigEntity();
    entity.setId(config.id());
    entity.setStepType(config.stepType().name());
    entity.setIsEnabled(config.enabled());
    entity.setExecutionOrder(config.executionOrder());
    entity.setConfigurationPayload(config.configurationPayload());
    return entity;
  }
}
