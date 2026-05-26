package com.orasaka.core.application.service;

import com.orasaka.core.domain.model.job.JobInfo;
import com.orasaka.core.domain.model.job.JobStatus;
import com.orasaka.persistence.domain.model.JobDto;

/**
 * Package-private final mapper mapping JobDto to JobInfo. Follows ERR-107 (Mapper Isolation
 * Invariant).
 */
final class JobInfoMapper {

  private JobInfoMapper() {}

  static JobInfo toInfo(JobDto dto) {
    if (dto == null) {
      return null;
    }
    return new JobInfo(
        dto.id(),
        dto.userId(),
        dto.featureKey(),
        JobStatus.fromString(dto.status()),
        dto.payload(),
        dto.result(),
        dto.errorMessage(),
        dto.createdAt(),
        dto.updatedAt());
  }
}
