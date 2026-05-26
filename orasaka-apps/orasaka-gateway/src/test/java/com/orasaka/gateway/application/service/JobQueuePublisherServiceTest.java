package com.orasaka.gateway.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.gateway.infrastructure.support.SystemOverloadedException;
import com.orasaka.persistence.infrastructure.config.RabbitMQConfig;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

@ExtendWith(MockitoExtension.class)
class JobQueuePublisherServiceTest {

  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private JdbcClient jdbcClient;

  private JobQueuePublisherService service;

  @BeforeEach
  void setUp() {
    service = new JobQueuePublisherService(rabbitTemplate, jdbcClient);
  }

  @Test
  void constructor_nullRabbitTemplate_throws() {
    assertThrows(NullPointerException.class, () -> new JobQueuePublisherService(null, jdbcClient));
  }

  @Test
  void constructor_nullJdbcClient_throws() {
    assertThrows(
        NullPointerException.class, () -> new JobQueuePublisherService(rabbitTemplate, null));
  }

  @Test
  void publish_sendsMessageToRabbitTemplate() {
    var message = new JobMessage("job-1", "user-1", "feature", Map.of());
    service.publish(message);

    verify(rabbitTemplate)
        .convertAndSend(RabbitMQConfig.EXCHANGE, "orasaka.routing.text.process", message);
  }

  @Test
  void fallbackQueueFull_throwsSystemOverloadedException() {
    var message = new JobMessage("job-1", "user-1", "feature", Map.of());
    var cause = new RuntimeException("connection timeout");

    var ex =
        assertThrows(
            SystemOverloadedException.class, () -> service.fallbackQueueFull(message, cause));
    assertEquals("The system is currently overloaded. Please try again later.", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void publishApproval_sendsMessageToRabbitTemplate() {
    service.publishApproval("job-2", "user-2");

    verify(rabbitTemplate)
        .convertAndSend(
            "orasaka.automation.exchange",
            "job.approved",
            Map.of("jobId", "job-2", "userId", "user-2", "action", "APPROVED"));
  }

  @Test
  void publishApproval_withNullJobId_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> service.publishApproval(null, "user-2"));
  }

  @Test
  void publishApproval_withSanitizationNeeded() {
    service.publishApproval("job\r\n-2", "user-2");

    verify(rabbitTemplate)
        .convertAndSend(
            "orasaka.automation.exchange",
            "job.approved",
            Map.of("jobId", "job\r\n-2", "userId", "user-2", "action", "APPROVED"));
  }
}
