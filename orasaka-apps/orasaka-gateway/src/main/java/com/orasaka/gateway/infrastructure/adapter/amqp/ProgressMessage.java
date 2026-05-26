package com.orasaka.gateway.infrastructure.adapter.amqp;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable message model sent via RabbitMQ to report job progress. Enforces compact constructor
 * validation per ERR-106.
 */
public record ProgressMessage(String jobId, Integer progress) implements Serializable {

  public ProgressMessage {
    Objects.requireNonNull(jobId, "Job ID cannot be null");
    Objects.requireNonNull(progress, "Progress cannot be null");
    if (progress < 0 || progress > 100) {
      throw new IllegalArgumentException("Progress must be between 0 and 100");
    }
  }
}
