package com.orasaka.persistence.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.identity.domain.model.RateLimitDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.RateLimitEntity;
import org.junit.jupiter.api.Test;

class RateLimitPersistenceMapperTest {

  @Test
  void toDto_mapsAllFields() {
    var entity = new RateLimitEntity();
    entity.setTierKey("premium");
    entity.setRequestsPerMinute(100);
    entity.setConcurrentJobs(5);
    RateLimitDto dto = RateLimitPersistenceMapper.toDto(entity);
    assertEquals("premium", dto.tierKey());
    assertEquals(100, dto.requestsPerMinute());
    assertEquals(5, dto.concurrentJobs());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(RateLimitPersistenceMapper.toDto(null));
  }

  @Test
  void toEntity_mapsAllFields() {
    var dto = new RateLimitDto("free", 10, 1);
    var entity = RateLimitPersistenceMapper.toEntity(dto);
    assertEquals("free", entity.getTierKey());
    assertEquals(10, entity.getRequestsPerMinute());
    assertEquals(1, entity.getConcurrentJobs());
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(RateLimitPersistenceMapper.toEntity(null));
  }
}
