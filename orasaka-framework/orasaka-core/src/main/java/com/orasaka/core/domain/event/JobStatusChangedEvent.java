package com.orasaka.core.domain.event;

import com.orasaka.core.domain.model.job.JobInfo;
import java.util.Objects;

/** Core application event published when a job is created or its status changes. */
public record JobStatusChangedEvent(JobInfo job) {
  public JobStatusChangedEvent {
    Objects.requireNonNull(job, "JobInfo cannot be null");
  }
}
