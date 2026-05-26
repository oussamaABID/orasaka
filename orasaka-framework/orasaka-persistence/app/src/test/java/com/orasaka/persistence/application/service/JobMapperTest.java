package com.orasaka.persistence.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.domain.model.JobDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.JobEntity;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JobMapperTest {

  @Test
  void toDto_mapsAllFields() {
    var entity = new JobEntity();
    entity.setId("job-1");
    entity.setUserId("user-1");
    entity.setFeatureKey("video_gen");
    entity.setStatus("PENDING");
    entity.setPayload(Map.of("prompt", "test"));
    entity.setResult(Map.of("url", "http://result"));
    entity.setErrorMessage(null);
    Instant now = Instant.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    JobDto dto = JobMapper.toDto(entity);
    assertEquals("job-1", dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("video_gen", dto.featureKey());
    assertEquals("PENDING", dto.status());
    assertEquals(Map.of("prompt", "test"), dto.payload());
    assertEquals(Map.of("url", "http://result"), dto.result());
    assertNull(dto.errorMessage());
    assertEquals(now, dto.createdAt());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(JobMapper.toDto(null));
  }
}
