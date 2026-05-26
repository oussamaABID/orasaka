package com.orasaka.automation.application.service;

import com.orasaka.automation.domain.model.AutomationJobPayload;
import com.orasaka.automation.domain.model.AutomationJobStatus;
import com.orasaka.automation.infrastructure.config.AmqpConstants;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Consumes approved automation jobs from RabbitMQ and dispatches them to the appropriate connector
 * route based on the connector type.
 *
 * @since 2.0.0
 */
@Service
class JobConsumerService {
  private static final Logger logger = LoggerFactory.getLogger(JobConsumerService.class);

  private final RabbitTemplate rabbitTemplate;
  private final ConnectorDispatcher connectorDispatcher;

  JobConsumerService(RabbitTemplate rabbitTemplate, ConnectorDispatcher connectorDispatcher) {
    this.rabbitTemplate = rabbitTemplate;
    this.connectorDispatcher = connectorDispatcher;
  }

  @RabbitListener(queues = AmqpConstants.JOBS_QUEUE)
  void onJobReceived(AutomationJobPayload job) {
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
