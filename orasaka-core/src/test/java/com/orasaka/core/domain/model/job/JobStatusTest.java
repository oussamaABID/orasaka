package com.orasaka.core.domain.model.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JobStatusTest {

  @Test
  @DisplayName("fromString resolves all valid status values")
  void fromStringValid() {
    assertEquals(JobStatus.PENDING, JobStatus.fromString("PENDING"));
    assertEquals(JobStatus.PROCESSING, JobStatus.fromString("PROCESSING"));
    assertEquals(JobStatus.COMPLETED, JobStatus.fromString("COMPLETED"));
    assertEquals(JobStatus.FAILED, JobStatus.fromString("FAILED"));
  }

  @Test
  @DisplayName("fromString is case-insensitive")
  void fromStringCaseInsensitive() {
    assertEquals(JobStatus.PENDING, JobStatus.fromString("pending"));
    assertEquals(JobStatus.COMPLETED, JobStatus.fromString("Completed"));
    assertEquals(JobStatus.FAILED, JobStatus.fromString("fAiLeD"));
  }

  @Test
  @DisplayName("fromString rejects null")
  void fromStringNull() {
    assertThrows(IllegalArgumentException.class, () -> JobStatus.fromString(null));
  }

  @Test
  @DisplayName("fromString rejects blank")
  void fromStringBlank() {
    assertThrows(IllegalArgumentException.class, () -> JobStatus.fromString("  "));
  }

  @Test
  @DisplayName("fromString rejects unknown status")
  void fromStringUnknown() {
    assertThrows(IllegalArgumentException.class, () -> JobStatus.fromString("UNKNOWN"));
  }

  @Test
  @DisplayName("name() returns uppercase constant name")
  void nameReturnsCorrectString() {
    assertEquals("COMPLETED", JobStatus.COMPLETED.name());
    assertEquals("FAILED", JobStatus.FAILED.name());
  }
}
