package com.orasaka.core.domain.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.orasaka.core.domain.model.job.JobInfo;
import com.orasaka.core.domain.model.job.JobStatus;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JobStatusChangedEventTest {

  @Test
  @DisplayName("event requires non-null job info")
  void validation() {
    assertThrows(NullPointerException.class, () -> new JobStatusChangedEvent(null));

    Instant now = Instant.now();
    JobInfo jobInfo =
        new JobInfo(
            "job-1", "user-1", "feature-1", JobStatus.PENDING, Map.of(), Map.of(), null, now, now);

    JobStatusChangedEvent event = new JobStatusChangedEvent(jobInfo);
    assertEquals(jobInfo, event.job());
  }
}
