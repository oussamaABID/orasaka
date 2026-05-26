package com.orasaka.workers.integration.listener;

import com.orasaka.workers.integration.model.AutomationJobPayload;
import com.orasaka.workers.integration.model.AutomationJobStatus;
import com.orasaka.workers.integration.service.ConnectorDispatcher;
import com.orasaka.workers.shared.config.AmqpConstants;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Consumes approved automation jobs from RabbitMQ and dispatches them. */
@Component
public class AutomationJobListener {
  private static final Logger logger = LoggerFactory.getLogger(AutomationJobListener.class);

  private final RabbitTemplate rabbitTemplate;
  private final ConnectorDispatcher connectorDispatcher;

  public AutomationJobListener(
      RabbitTemplate rabbitTemplate, ConnectorDispatcher connectorDispatcher) {
    this.rabbitTemplate = rabbitTemplate;
    this.connectorDispatcher = connectorDispatcher;
  }

  @RabbitListener(queues = AmqpConstants.JOBS_QUEUE)
  public void onJobReceived(AutomationJobPayload job) {
    logger.info(
        "Received approved job: id={}, connector={}, action={}",
        job.jobId(),
        job.connectorType(),
        job.action());

    publishTelemetry(job.jobId(), AutomationJobStatus.RUNNING, "Job execution started");

    try {
      connectorDispatcher.dispatch(job);
      publishTelemetry(job.jobId(), AutomationJobStatus.COMPLETED, "Job completed successfully");
      logger.info("Job {} completed successfully.", job.jobId());
    } catch (Exception e) {
      publishTelemetry(job.jobId(), AutomationJobStatus.FAILED, "Error: " + e.getMessage());
      logger.error("Job {} failed.", job.jobId(), e);
    }
  }

  private void publishTelemetry(String jobId, AutomationJobStatus status, String message) {
    Map<String, Object> telemetry =
        Map.of("jobId", jobId, "status", status.name(), "message", message);
    rabbitTemplate.convertAndSend(
        AmqpConstants.AUTOMATION_EXCHANGE, AmqpConstants.TELEMETRY_ROUTING_KEY, telemetry);
  }
}
