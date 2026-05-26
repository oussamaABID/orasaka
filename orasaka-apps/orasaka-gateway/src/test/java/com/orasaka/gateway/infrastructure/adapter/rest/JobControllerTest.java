package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.job.JobInfo;
import com.orasaka.core.domain.model.job.JobStatus;
import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.application.service.JobStreamService;
import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class JobControllerTest {

  private JobService jobService;
  private JobQueuePublisherService jobQueuePublisher;
  private JobStreamService jobStreamService;
  private JobController jobController;

  @BeforeEach
  void setUp() {
    jobService = mock(JobService.class);
    jobQueuePublisher = mock(JobQueuePublisherService.class);
    jobStreamService = mock(JobStreamService.class);
    jobController = new JobController(jobService, jobQueuePublisher, jobStreamService);
  }

  @Test
  void testSubmitJob() {
    UUID userId = UUID.randomUUID();
    User user = new User(userId, "test", "test@test.class", true, Set.of("ROLE_USER"), Map.of());
    Map<String, Object> payload = Map.of("input", "data");
    JobController.SubmitJobRequest request = new JobController.SubmitJobRequest("video", payload);

    when(jobService.createJob(userId.toString(), "video", payload)).thenReturn("job-123");

    ResponseEntity<Map<String, Object>> response = jobController.submitJob(request, user);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("job-123", response.getBody().get("jobId"));
    assertEquals("PENDING", response.getBody().get("status"));
    verify(jobQueuePublisher).publish(any(JobMessage.class));
  }

  @Test
  void testSubmitJobRequestNullKey() {
    Map<String, Object> payload = Map.of();
    assertThrows(
        NullPointerException.class, () -> new JobController.SubmitJobRequest(null, payload));
  }

  @Test
  void testGetJobsAdmin() {
    UUID userId = UUID.randomUUID();
    User user = new User(userId, "admin", "admin@test.class", true, Set.of("ROLE_ADMIN"), Map.of());
    Page<JobInfo> expectedPage =
        new PageImpl<>(
            List.of(
                new JobInfo(
                    "job-1",
                    userId.toString(),
                    "video",
                    JobStatus.PROCESSING,
                    Map.of(),
                    Map.of(),
                    null,
                    Instant.now(),
                    Instant.now())));
    when(jobService.getAllJobs(any(PageRequest.class))).thenReturn(expectedPage);

    ResponseEntity<Page<JobInfo>> response = jobController.getJobs(0, 10, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedPage, response.getBody());
    verify(jobService).getAllJobs(any(PageRequest.class));
    verify(jobService, never()).getJobsByUserId(anyString(), any(PageRequest.class));
  }

  @Test
  void testGetJobsUser() {
    UUID userId = UUID.randomUUID();
    User user = new User(userId, "user", "user@test.class", true, Set.of("ROLE_USER"), Map.of());
    Page<JobInfo> expectedPage =
        new PageImpl<>(
            List.of(
                new JobInfo(
                    "job-1",
                    userId.toString(),
                    "video",
                    JobStatus.PROCESSING,
                    Map.of(),
                    Map.of(),
                    null,
                    Instant.now(),
                    Instant.now())));
    when(jobService.getJobsByUserId(eq(userId.toString()), any(PageRequest.class)))
        .thenReturn(expectedPage);

    ResponseEntity<Page<JobInfo>> response = jobController.getJobs(0, 10, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedPage, response.getBody());
    verify(jobService, never()).getAllJobs(any(PageRequest.class));
    verify(jobService).getJobsByUserId(eq(userId.toString()), any(PageRequest.class));
  }

  @Test
  void testGetJobFound() {
    JobInfo info =
        new JobInfo(
            "job-1",
            "user-1",
            "video",
            JobStatus.COMPLETED,
            Map.of(),
            Map.of(),
            null,
            Instant.now(),
            Instant.now());
    when(jobService.getJob("job-1")).thenReturn(Optional.of(info));

    ResponseEntity<JobInfo> response = jobController.getJob("job-1");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(info, response.getBody());
  }

  @Test
  void testGetJobNotFound() {
    when(jobService.getJob("job-1")).thenReturn(Optional.empty());

    ResponseEntity<JobInfo> response = jobController.getJob("job-1");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void testStreamJobs() {
    UUID userId = UUID.randomUUID();
    User user = new User(userId, "user", "user@test.class", true, Set.of("ROLE_USER"), Map.of());
    SseEmitter emitter = new SseEmitter();
    when(jobStreamService.register(userId.toString())).thenReturn(emitter);

    SseEmitter result = jobController.streamJobs(user);

    assertEquals(emitter, result);
  }

  @Test
  void testJobProgressRequestValidation() {
    // Valid
    var req = new JobController.JobProgressRequest(50);
    assertEquals(50, req.progress());

    // Null
    assertThrows(NullPointerException.class, () -> new JobController.JobProgressRequest(null));

    // Negative
    assertThrows(InvalidRequestException.class, () -> new JobController.JobProgressRequest(-5));

    // Too high
    assertThrows(InvalidRequestException.class, () -> new JobController.JobProgressRequest(105));
  }

  @Test
  void testUpdateJobProgressFound() {
    JobInfo info =
        new JobInfo(
            "job-1",
            "user-1",
            "video",
            JobStatus.PROCESSING,
            Map.of(),
            Map.of(),
            null,
            Instant.now(),
            Instant.now());
    when(jobService.getJob("job-1")).thenReturn(Optional.of(info));

    ResponseEntity<Void> response =
        jobController.updateJobProgress("job-1", new JobController.JobProgressRequest(40));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(jobStreamService).broadcastProgress("job-1", "user-1", 40);
  }

  @Test
  void testUpdateJobProgressNotFound() {
    when(jobService.getJob("job-1")).thenReturn(Optional.empty());

    ResponseEntity<Void> response =
        jobController.updateJobProgress("job-1", new JobController.JobProgressRequest(40));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(jobStreamService, never()).broadcastProgress(anyString(), anyString(), anyInt());
  }
}
