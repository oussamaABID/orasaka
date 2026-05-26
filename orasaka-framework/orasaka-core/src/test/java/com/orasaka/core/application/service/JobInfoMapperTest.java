package com.orasaka.core.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.job.JobInfo;
import com.orasaka.core.domain.model.job.JobStatus;
import com.orasaka.persistence.domain.model.JobDto;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JobInfoMapperTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void toInfo_null_returnsNull() {
    assertNull(JobInfoMapper.toInfo(null));
  }

  @Test
  void toInfo_validDto_mapsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto =
        new JobDto(
            "job-1",
            "user-1",
            "video_generation",
            "PENDING",
            Map.of("prompt", "test"),
            null,
            null,
            now,
            now);

    JobInfo result = JobInfoMapper.toInfo(dto);

    assertNotNull(result);
    assertEquals("job-1", result.id());
    assertEquals("user-1", result.userId());
    assertEquals("video_generation", result.featureKey());
    assertEquals(JobStatus.PENDING, result.status());
    assertEquals(Map.of("prompt", "test"), result.payload());
    assertEquals(Map.of(), result.result());
    assertNull(result.errorMessage());
    assertEquals(now, result.createdAt());
    assertEquals(now, result.updatedAt());
  }

  @Test
  void toInfo_completedStatus_mapsCorrectly() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto =
        new JobDto(
            "job-2",
            "user-1",
            "image_gen",
            "COMPLETED",
            Map.of(),
            Map.of("url", "http://result"),
            null,
            now,
            now);

    JobInfo result = JobInfoMapper.toInfo(dto);
    assertEquals(JobStatus.COMPLETED, result.status());
    assertEquals(Map.of("url", "http://result"), result.result());
  }

  @Test
  void toInfo_failedStatus_mapsErrorMessage() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto =
        new JobDto(
            "job-3",
            "user-1",
            "video_gen",
            "FAILED",
            Map.of(),
            null,
            "Something went wrong",
            now,
            now);

    JobInfo result = JobInfoMapper.toInfo(dto);
    assertEquals(JobStatus.FAILED, result.status());
    assertEquals("Something went wrong", result.errorMessage());
  }
}
