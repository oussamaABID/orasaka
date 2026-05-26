package com.orasaka.workers.integration.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orasaka.workers.integration.model.AutomationJobPayload;
import com.orasaka.workers.integration.model.AutomationJobStatus;
import com.orasaka.workers.integration.service.ConnectorDispatcher;
import com.orasaka.workers.shared.config.AmqpConstants;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class AutomationJobListenerTest {

  private static final String STATUS_KEY = "status";

  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private ConnectorDispatcher connectorDispatcher;
  private AutomationJobListener listener;

  @BeforeEach
  void setUp() {
    listener = new AutomationJobListener(rabbitTemplate, connectorDispatcher);
  }

  private AutomationJobPayload createJob() {
    return new AutomationJobPayload(
        "job-100",
        "user-1",
        "JIRA",
        "create-issue",
        AutomationJobStatus.APPROVED,
        Map.of("summary", "Fix bug"),
        Instant.now());
  }

  @Test
  @DisplayName("Should publish RUNNING telemetry then delegate to dispatcher on success")
  @SuppressWarnings("unchecked")
  void successfulJobLifecycle() {
    AutomationJobPayload job = createJob();

    listener.onJobReceived(job);

    // Verify dispatcher was called
    verify(connectorDispatcher).dispatch(job);

    // Verify 2 telemetry events: RUNNING then COMPLETED
    ArgumentCaptor<Map<String, Object>> telemetryCaptor = ArgumentCaptor.forClass(Map.class);
    verify(rabbitTemplate, times(2))
        .convertAndSend(
            eq(AmqpConstants.AUTOMATION_EXCHANGE),
            eq(AmqpConstants.TELEMETRY_ROUTING_KEY),
            telemetryCaptor.capture());

    // First telemetry = RUNNING
    Map<String, Object> runningTelemetry = telemetryCaptor.getAllValues().get(0);
    assertThat(runningTelemetry)
        .containsEntry(STATUS_KEY, "RUNNING")
        .containsEntry("jobId", "job-100");

    // Second telemetry = COMPLETED
    Map<String, Object> completedTelemetry = telemetryCaptor.getAllValues().get(1);
    assertThat(completedTelemetry).containsEntry(STATUS_KEY, "COMPLETED");
  }

  @Test
  @DisplayName("Should publish FAILED telemetry when dispatcher throws")
  @SuppressWarnings("unchecked")
  void failedJobPublishesFailedTelemetry() {
    AutomationJobPayload job = createJob();
    doThrow(new RuntimeException("Connector unavailable")).when(connectorDispatcher).dispatch(job);

    listener.onJobReceived(job);

    // Verify telemetry: first RUNNING, then FAILED
    ArgumentCaptor<Map<String, Object>> telemetryCaptor = ArgumentCaptor.forClass(Map.class);
    verify(rabbitTemplate, times(2))
        .convertAndSend(
            eq(AmqpConstants.AUTOMATION_EXCHANGE),
            eq(AmqpConstants.TELEMETRY_ROUTING_KEY),
            telemetryCaptor.capture());

    Map<String, Object> failedTelemetry = telemetryCaptor.getAllValues().get(1);
    assertThat(failedTelemetry).containsEntry(STATUS_KEY, "FAILED");
    assertThat((String) failedTelemetry.get("message")).contains("Connector unavailable");
  }

  @Test
  @DisplayName("Should always publish at least 2 telemetry events per job")
  void alwaysPublishesTwoTelemetryEvents() {
    listener.onJobReceived(createJob());

    verify(rabbitTemplate, times(2))
        .convertAndSend(
            eq(AmqpConstants.AUTOMATION_EXCHANGE),
            eq(AmqpConstants.TELEMETRY_ROUTING_KEY),
            (Object) any());
  }
}
