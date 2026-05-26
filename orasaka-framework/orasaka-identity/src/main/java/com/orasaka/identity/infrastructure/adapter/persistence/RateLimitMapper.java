package com.orasaka.identity.infrastructure.adapter.persistence;

import com.orasaka.identity.domain.model.RateLimitInfo;
import com.orasaka.persistence.identity.domain.model.RateLimitDto;

/** Package-private final mapper utility mapping RateLimitDto to RateLimitInfo. Follows ERR-107. */
final class RateLimitMapper {

  private RateLimitMapper() {}

  static RateLimitInfo toInfo(RateLimitDto dto) {
    if (dto == null) {
      return null;
    }
    return new RateLimitInfo(dto.tierKey(), dto.requestsPerMinute(), dto.concurrentJobs());
  }
}
