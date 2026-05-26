package com.orasaka.core.domain.model.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JobInfoTest {

  @Test
  @DisplayName("JobInfo compact constructor validates all inputs")
  void constructorValidation() {
    Instant now = Instant.now();

    // Valid inputs succeed
    JobInfo valid =
        new JobInfo("job-1", "user-1", "feature-1", JobStatus.PENDING, null, null, null, now, now);
    assertNotNull(valid);
    assertEquals(Map.of(), valid.payload());
    assertEquals(Map.of(), valid.result());

    // Null/blank checks
    assertThrows(
        NullPointerException.class,
        () -> new JobInfo(null, "user-1", "feat", JobStatus.PENDING, null, null, null, now, now));
    assertThrows(
        IllegalArgumentException.class,
        () -> new JobInfo("", "user-1", "feat", JobStatus.PENDING, null, null, null, now, now));
    assertThrows(
        IllegalArgumentException.class,
        () -> new JobInfo("  ", "user-1", "feat", JobStatus.PENDING, null, null, null, now, now));

    assertThrows(
        NullPointerException.class,
        () -> new JobInfo("job-1", "user-1", null, JobStatus.PENDING, null, null, null, now, now));
    assertThrows(
        IllegalArgumentException.class,
        () -> new JobInfo("job-1", "user-1", "", JobStatus.PENDING, null, null, null, now, now));

    assertThrows(
        NullPointerException.class,
        () -> new JobInfo("job-1", "user-1", "feat", null, null, null, null, now, now));

    assertThrows(
        NullPointerException.class,
        () ->
            new JobInfo("job-1", "user-1", "feat", JobStatus.PENDING, null, null, null, null, now));
    assertThrows(
        NullPointerException.class,
        () ->
            new JobInfo("job-1", "user-1", "feat", JobStatus.PENDING, null, null, null, now, null));
  }

  @Test
  @DisplayName("JobInfo performs defensive copies of maps")
  void defensiveCopy() {
    Instant now = Instant.now();
    Map<String, Object> payload = new HashMap<>();
    payload.put("a", 1);
    Map<String, Object> result = new HashMap<>();
    result.put("b", 2);

    JobInfo info =
        new JobInfo("job-1", "user-1", "feat", JobStatus.PENDING, payload, result, null, now, now);

    // Modify original map
    payload.put("a", 99);
    result.put("b", 100);

    // Assert that the record's maps remain unchanged
    assertEquals(1, info.payload().get("a"));
    assertEquals(2, info.result().get("b"));

    // Assert immutability of returned maps
    var payloadMap = info.payload();
    var resultMap = info.result();
    assertThrows(UnsupportedOperationException.class, () -> payloadMap.put("x", 1));
    assertThrows(UnsupportedOperationException.class, () -> resultMap.put("y", 2));
  }
}
