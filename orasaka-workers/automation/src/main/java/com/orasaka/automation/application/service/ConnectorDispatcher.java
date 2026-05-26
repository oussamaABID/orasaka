package com.orasaka.automation.application.service;

import com.orasaka.automation.domain.model.AutomationJobPayload;
import com.orasaka.automation.infrastructure.config.AmqpConstants;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Dispatches automation job payloads to the appropriate connector route based on the connector
 * type.
 *
 * @since 2.0.0
 */
@Service
class ConnectorDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(ConnectorDispatcher.class);

  private final RabbitTemplate rabbitTemplate;

  ConnectorDispatcher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Routes the job payload to the correct connector execution logic.
   *
   * @param job The automation job payload.
   */
  void dispatch(AutomationJobPayload job) {
    switch (job.connectorType().toUpperCase()) {
      case "JIRA" -> executeJiraAction(job);
      case "WHATSAPP" -> executeWhatsAppAction(job);
      case "MESSENGER" -> executeMessengerAction(job);
      case "SLACK" -> executeSlackAction(job);
      case "CLI_AGENT" -> dispatchToCliAgent(job);
      default ->
          throw new IllegalArgumentException("Unknown connector type: " + job.connectorType());
    }
  }

  private void executeJiraAction(AutomationJobPayload job) {
    logger.info(
        "Executing Jira action '{}' for job {}: {}", job.action(), job.jobId(), job.payload());
    // Apache Camel Jira route integration point
  }

  private void executeWhatsAppAction(AutomationJobPayload job) {
    logger.info(
        "Executing WhatsApp action '{}' for job {}: {}", job.action(), job.jobId(), job.payload());
    // WhatsApp Cloud Graph API HTTP call
  }

  private void executeMessengerAction(AutomationJobPayload job) {
    logger.info(
        "Executing Messenger action '{}' for job {}: {}", job.action(), job.jobId(), job.payload());
    // Messenger Platform API call
  }

  private void executeSlackAction(AutomationJobPayload job) {
    logger.info(
        "Executing Slack action '{}' for job {}: {}", job.action(), job.jobId(), job.payload());
    // Slack Web API call
  }

  private void dispatchToCliAgent(AutomationJobPayload job) {
    logger.info("Dispatching CLI agent payload for job {} to user {}", job.jobId(), job.userId());
    String routingKey = "cli." + job.userId() + ".dispatch";
    rabbitTemplate.convertAndSend(
        AmqpConstants.AUTOMATION_EXCHANGE,
        routingKey,
        Map.of(
            "jobId", job.jobId(),
            "userId", job.userId(),
            "action", job.action(),
            "payload", job.payload()));
  }
}
