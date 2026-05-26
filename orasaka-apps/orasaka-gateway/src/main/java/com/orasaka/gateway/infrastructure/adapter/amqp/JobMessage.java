package com.orasaka.gateway.infrastructure.adapter.amqp;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/** Immutable message model sent via RabbitMQ to request task execution. */
public record JobMessage(
    String jobId, String userId, String featureKey, String model, Map<String, Object> payload)
    implements Serializable {

  public JobMessage {
    Objects.requireNonNull(jobId, "Job ID cannot be null");
    Objects.requireNonNull(featureKey, "Feature key cannot be null");
    Objects.requireNonNull(model, "Model cannot be null");
    payload = (payload != null) ? Map.copyOf(payload) : Map.of();
  }

  /** Overloaded constructor for backward compatibility with 4-argument usages. */
  public JobMessage(String jobId, String userId, String featureKey, Map<String, Object> payload) {
    this(jobId, userId, featureKey, "default", payload);
  }
}
