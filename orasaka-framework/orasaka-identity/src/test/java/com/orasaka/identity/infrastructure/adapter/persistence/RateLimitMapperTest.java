package com.orasaka.identity.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.domain.model.RateLimitInfo;
import com.orasaka.persistence.identity.domain.model.RateLimitDto;
import org.junit.jupiter.api.Test;

class RateLimitMapperTest {

  @Test
  void toInfo_mapsAllFields() {
    var dto = new RateLimitDto("premium", 100, 5);
    RateLimitInfo info = RateLimitMapper.toInfo(dto);
    assertEquals("premium", info.tierKey());
    assertEquals(100, info.requestsPerMinute());
    assertEquals(5, info.concurrentJobs());
  }

  @Test
  void toInfo_null_returnsNull() {
    assertNull(RateLimitMapper.toInfo(null));
  }
}
