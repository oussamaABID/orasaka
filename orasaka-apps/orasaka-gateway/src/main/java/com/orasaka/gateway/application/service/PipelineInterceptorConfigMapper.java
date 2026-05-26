package com.orasaka.gateway.application.service;

import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PipelineInterceptorConfigEntity;

/** Package-private static mapper for pipeline interceptor config entities ↔ domain records. */
final class PipelineInterceptorConfigMapper {

  private PipelineInterceptorConfigMapper() {}

  /**
   * Maps a JPA entity to a domain record.
   *
   * @param entity The JPA entity.
   * @return The domain record.
   */
  static InterceptorConfig toRecord(PipelineInterceptorConfigEntity entity) {
    return new InterceptorConfig(
        entity.getInterceptorKey(),
        entity.getDisplayLabel(),
        entity.getExecutionOrder(),
        Boolean.TRUE.equals(entity.getIsEnabled()),
        entity.getDescription());
  }

  /**
   * Maps a domain record to a JPA entity.
   *
   * @param config The domain record.
   * @return A new JPA entity populated from the record.
   */
  static PipelineInterceptorConfigEntity toEntity(InterceptorConfig config) {
    var entity = new PipelineInterceptorConfigEntity();
    entity.setInterceptorKey(config.interceptorKey());
    entity.setDisplayLabel(config.displayLabel());
    entity.setExecutionOrder(config.executionOrder());
    entity.setIsEnabled(config.enabled());
    entity.setDescription(config.description());
    return entity;
  }
}
