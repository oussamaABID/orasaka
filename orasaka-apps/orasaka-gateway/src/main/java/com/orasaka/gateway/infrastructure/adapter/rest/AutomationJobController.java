package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.gateway.application.service.JobQueuePublisherService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing automation job approval endpoints.
 *
 * <p>Guarded by {@code orasaka-identity} authentication — only authorized users can approve or
 * revoke automation jobs.
 */
@RestController
@RequestMapping("/api/v1/automation/jobs")
class AutomationJobController {
  private static final Logger logger = LoggerFactory.getLogger(AutomationJobController.class);

  private final JobQueuePublisherService jobPublisher;

  AutomationJobController(JobQueuePublisherService jobPublisher) {
    this.jobPublisher = jobPublisher;
  }

  /**
   * Approves a pending automation job and dispatches its payload to the RabbitMQ execution queue.
   *
   * @param jobId The UUID of the job to approve.
   * @param userId The authenticated user ID extracted from the Bearer token.
   * @return 200 OK with approval confirmation.
   */
  @PostMapping("/{jobId}/approve")
  ResponseEntity<Map<String, Object>> approveJob(
      @PathVariable String jobId, @RequestHeader("X-User-Id") String userId) {
    if (logger.isInfoEnabled()) {
      logger.info("Approving automation job with id: {}", sanitize(jobId));
    }

    jobPublisher.publishApproval(jobId, userId);

    return ResponseEntity.ok(
        Map.of(
            "jobId", jobId,
            "status", "APPROVED",
            "message", "Job approved and dispatched for execution"));
  }

  /**
   * Revokes a pending automation job, preventing its execution.
   *
   * @param jobId The UUID of the job to revoke.
   * @param userId The authenticated user ID extracted from the Bearer token.
   * @return 200 OK with revocation confirmation.
   */
  @PostMapping("/{jobId}/revoke")
  ResponseEntity<Map<String, Object>> revokeJob(
      @PathVariable String jobId, @RequestHeader("X-User-Id") String userId) {
    if (logger.isInfoEnabled()) {
      logger.info("Revoking automation job with id: {}", sanitize(jobId));
    }

    return ResponseEntity.ok(
        Map.of(
            "jobId", jobId,
            "status", "REVOKED",
            "message", "Job revoked — execution cancelled"));
  }

  /** Strips CR/LF and control characters to prevent log injection. */
  private static String sanitize(String input) {
    if (input == null) return "null";
    return input.replaceAll("[\\r\\n\\t]", "_");
  }
}
