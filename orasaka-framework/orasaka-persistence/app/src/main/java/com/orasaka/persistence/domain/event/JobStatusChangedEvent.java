package com.orasaka.persistence.domain.event;

import com.orasaka.persistence.domain.model.JobDto;
import java.util.Objects;

/** Application event published when a job is created or its status changes. */
public record JobStatusChangedEvent(JobDto job) {
  public JobStatusChangedEvent {
    Objects.requireNonNull(job, "Job DTO cannot be null");
  }
}
