package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class VideoControllerTest {

  @TempDir Path tempDir;

  @Mock private JobService jobService;

  @Mock private JobQueuePublisherService jobQueuePublisher;

  private VideoController controller;

  @BeforeEach
  void setUp() {
    controller = new VideoController(jobService, jobQueuePublisher, tempDir.toString());
  }

  @Test
  void generateVideo_validRequest_createsAndPublishesJob() {
    UUID userId = UUID.randomUUID();
    User user =
        new User(userId, "testuser", "test@example.com", true, Set.of("ROLE_USER"), Map.of());

    VideoGenerationRequest request =
        new VideoGenerationRequest(
            "Generate a cat video", 10, 24, 30, "animatediff-lightning-mps", null, Map.of());

    ResponseEntity<Map<String, Object>> response = controller.generateVideo(request, user);

    assertNotNull(response);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue(body.containsKey("jobId"));
    assertEquals("PENDING", body.get("status"));

    verify(jobService)
        .createJob(anyString(), eq(userId.toString()), eq("orasaka.core.media.video"), anyMap());
    verify(jobQueuePublisher).publish(any(JobMessage.class));
  }

  @Test
  void generateVideo_invalidModel_throwsInvalidRequestException() {
    UUID userId = UUID.randomUUID();
    User user =
        new User(userId, "testuser", "test@example.com", true, Set.of("ROLE_USER"), Map.of());

    VideoGenerationRequest request =
        new VideoGenerationRequest(
            "Generate a cat video", 10, 24, 30, "non-existent-model", null, Map.of());

    assertThrows(InvalidRequestException.class, () -> controller.generateVideo(request, user));
    verify(jobService, never()).createJob(any(), any(), any(), any());
    verify(jobQueuePublisher, never()).publish(any());
  }
}
