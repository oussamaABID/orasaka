package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.model.job.JobInfo;
import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.application.service.JobStreamService;
import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for asynchronous framework tasks/jobs. Exposes endpoints to submit, query, and
 * stream jobs. Decoupled from persistence module per ERR-102.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

  private static final Logger logger = LoggerFactory.getLogger(JobController.class);

  private final JobService jobService;
  private final JobQueuePublisherService jobQueuePublisher;
  private final JobStreamService jobStreamService;

  public JobController(
      JobService jobService,
      JobQueuePublisherService jobQueuePublisher,
      JobStreamService jobStreamService) {
    this.jobService = Objects.requireNonNull(jobService, "JobService cannot be null");
    this.jobQueuePublisher =
        Objects.requireNonNull(jobQueuePublisher, "JobQueuePublisherService cannot be null");
    this.jobStreamService =
        Objects.requireNonNull(jobStreamService, "JobStreamService cannot be null");
  }

  /** Request DTO for job submission. */
  public static record SubmitJobRequest(String featureKey, Map<String, Object> payload) {
    public SubmitJobRequest {
      Objects.requireNonNull(featureKey, "Feature key is required");
    }
  }

  /**
   * Submits a job asynchronously. Instantiates job state to PENDING and publishes message to
   * RabbitMQ, returning 202 Accepted.
   *
   * @param request The job submission parameters.
   * @param user The authenticated user principal.
   * @return Enqueued job status descriptor.
   */
  @PostMapping
  public ResponseEntity<Map<String, Object>> submitJob(
      @RequestBody SubmitJobRequest request, @AuthenticationPrincipal User user) {
    logger.debug("Received async task submission request for feature: {}", request.featureKey());
    String userId = user.id().toString();

    String jobId = jobService.createJob(userId, request.featureKey(), request.payload());

    JobMessage message = new JobMessage(jobId, userId, request.featureKey(), request.payload());
    jobQueuePublisher.publish(message);

    logger.debug("Successfully enqueued task in RabbitMQ. Job ID: {}", jobId);

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(Map.of("jobId", jobId, "status", "PENDING"));
  }

  /**
   * Retrieves a paginated list of all jobs belonging to the authenticated user.
   *
   * @param page Target page index.
   * @param size Page size.
   * @param user The authenticated user principal.
   * @return Page containing user jobs.
   */
  @GetMapping
  public ResponseEntity<Page<JobInfo>> getJobs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @AuthenticationPrincipal User user) {
    logger.debug("Fetching paginated jobs for user: {}, page: {}, size: {}", user.id(), page, size);

    PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<JobInfo> jobs;
    if (user.authorities().contains("ROLE_ADMIN")) {
      logger.debug("Principal is ADMIN. Bypassing user filter to query all platform jobs.");
      jobs = jobService.getAllJobs(pageable);
    } else {
      jobs = jobService.getJobsByUserId(user.id().toString(), pageable);
    }
    return ResponseEntity.ok(jobs);
  }

  /** Fetches the single, atomic state of a specific job. */
  @GetMapping("/{id}")
  public ResponseEntity<JobInfo> getJob(@PathVariable String id) {
    logger.debug("Resolving status for job ID: {}", id);
    return jobService
        .getJob(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Registers a Server-Sent Events stream for real-time job status notifications.
   *
   * @param user The authenticated user principal.
   * @return SSE emitter for real-time job events.
   */
  @GetMapping("/stream")
  public SseEmitter streamJobs(@AuthenticationPrincipal User user) {
    logger.debug("Registering SSE emitter for user: {}", user.id());
    return jobStreamService.register(user.id().toString());
  }

  /** Request DTO for job progress updates. */
  public static record JobProgressRequest(Integer progress) {
    public JobProgressRequest {
      Objects.requireNonNull(progress, "Progress percentage is required");
      if (progress < 0 || progress > 100) {
        throw new InvalidRequestException("Progress must be between 0 and 100");
      }
    }
  }

  /** Updates progress of a running job and broadcasts it to connected users. */
  @PostMapping("/{id}/progress")
  public ResponseEntity<Void> updateJobProgress(
      @PathVariable String id, @RequestBody JobProgressRequest request) {
    logger.debug("Received progress update for job ID: {}, progress: {}%", id, request.progress());
    jobService
        .getJob(id)
        .ifPresent(job -> jobStreamService.broadcastProgress(id, job.userId(), request.progress()));
    return ResponseEntity.ok().build();
  }
}
