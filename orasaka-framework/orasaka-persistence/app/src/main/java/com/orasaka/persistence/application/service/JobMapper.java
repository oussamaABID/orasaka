package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.model.JobDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.JobEntity;

/** Package-private final mapper mapping JobEntity to JobDto per ERR-107. */
final class JobMapper {

  private JobMapper() {
    // Utility class private constructor
  }

  static JobDto toDto(JobEntity entity) {
    if (entity == null) {
      return null;
    }
    return new JobDto(
        entity.getId(),
        entity.getUserId(),
        entity.getFeatureKey(),
        entity.getStatus(),
        entity.getPayload(),
        entity.getResult(),
        entity.getErrorMessage(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
