package com.orasaka.automation.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable record representing an automation job payload dispatched via RabbitMQ.
 *
 * @param jobId Unique job identifier.
 * @param userId Owner of the job.
 * @param connectorType Target connector (JIRA, WHATSAPP, MESSENGER, SLACK, CLI_AGENT).
 * @param action Specific action to execute.
 * @param status Current lifecycle status.
 * @param payload Arbitrary key-value parameters for execution.
 * @param createdAt Timestamp of job creation.
 * @since 2.0.0
 */
public record AutomationJobPayload(
    String jobId,
    String userId,
    String connectorType,
    String action,
    AutomationJobStatus status,
    Map<String, Object> payload,
    Instant createdAt) {

  public AutomationJobPayload {
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(connectorType, "connectorType must not be null");
    Objects.requireNonNull(action, "action must not be null");
    Objects.requireNonNull(status, "status must not be null");
    payload = payload != null ? Map.copyOf(payload) : Map.of();
    createdAt = createdAt != null ? createdAt : Instant.now();
  }
}
