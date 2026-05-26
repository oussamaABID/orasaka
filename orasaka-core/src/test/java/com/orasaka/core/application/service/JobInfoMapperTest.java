package com.orasaka.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.orasaka.core.domain.model.job.JobInfo;
import com.orasaka.core.domain.model.job.JobStatus;
import com.orasaka.persistence.domain.model.JobDto;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JobInfoMapperTest {

  @Test
  @DisplayName("map to info handles null and maps correct values")
  void mapToInfo() {
    assertNull(JobInfoMapper.toInfo(null));

    Instant now = Instant.now();
    JobDto dto =
        new JobDto(
            "job-1",
            "user-1",
            "feature-1",
            "PENDING",
            Map.of("input", "val"),
            Map.of("out", "res"),
            "error message",
            now,
            now);
    JobInfo info = JobInfoMapper.toInfo(dto);

    assertEquals("job-1", info.id());
    assertEquals("user-1", info.userId());
    assertEquals("feature-1", info.featureKey());
    assertEquals(JobStatus.PENDING, info.status());
    assertEquals(Map.of("input", "val"), info.payload());
    assertEquals(Map.of("out", "res"), info.result());
    assertEquals("error message", info.errorMessage());
    assertEquals(now, info.createdAt());
    assertEquals(now, info.updatedAt());
  }
}
