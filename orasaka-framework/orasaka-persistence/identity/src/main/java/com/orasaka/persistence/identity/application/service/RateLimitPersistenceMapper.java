package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.RateLimitDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.RateLimitEntity;

/** Package-private static mapper for RateLimit entities, satisfying ERR-107. */
final class RateLimitPersistenceMapper {

  private RateLimitPersistenceMapper() {}

  static RateLimitDto toDto(RateLimitEntity entity) {
    if (entity == null) {
      return null;
    }
    return new RateLimitDto(
        entity.getTierKey(), entity.getRequestsPerMinute(), entity.getConcurrentJobs());
  }

  static RateLimitEntity toEntity(RateLimitDto dto) {
    if (dto == null) {
      return null;
    }
    RateLimitEntity entity = new RateLimitEntity();
    entity.setTierKey(dto.tierKey());
    entity.setRequestsPerMinute(dto.requestsPerMinute());
    entity.setConcurrentJobs(dto.concurrentJobs());
    return entity;
  }
}
