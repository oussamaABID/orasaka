package com.orasaka.gateway.application.service;

import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.gateway.infrastructure.support.SystemOverloadedException;
import com.orasaka.persistence.infrastructure.config.RabbitMQConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Service component responsible for publishing jobs to the RabbitMQ broker, protected by a
 * Resilience4j circuit breaker.
 */
@Service
public class JobQueuePublisherService {

  private static final Logger logger = LoggerFactory.getLogger(JobQueuePublisherService.class);

  private final RabbitTemplate rabbitTemplate;
  private final JdbcClient jdbcClient;

  public JobQueuePublisherService(RabbitTemplate rabbitTemplate, JdbcClient jdbcClient) {
    this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "RabbitTemplate cannot be null");
    this.jdbcClient = Objects.requireNonNull(jdbcClient, "JdbcClient cannot be null");
  }

  /**
   * Publishes a job message to RabbitMQ within a circuit breaker.
   *
   * @param message the job message to publish
   * @throws SystemOverloadedException if the broker queue is full or unreachable
   */
  @CircuitBreaker(name = "backendBrokerCB", fallbackMethod = "fallbackQueueFull")
  public void publish(JobMessage message) {
    logger.debug("Publishing job message to RabbitMQ: {}", message.jobId());
    String routingKey = resolveRoutingKey(message);
    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, message);
  }

  private String resolveRoutingKey(JobMessage message) {
    String featureKey = message.featureKey();
    String userId = message.userId();

    if (userId != null && featureKey != null) {
      try {
        return jdbcClient
            .sql(
                "SELECT r.target_routing_key FROM orasaka_routing_rules r "
                    + "JOIN orasaka_users u ON r.user_tier = u.rate_limit_tier "
                    + "WHERE r.feature_key = ? AND u.id = ? AND r.is_active = true "
                    + "LIMIT 1")
            .param(1, featureKey)
            .param(2, userId)
            .query(String.class)
            .optional()
            .orElseGet(() -> getDefaultRoutingKey(featureKey));
      } catch (Exception e) {
        logger.warn(
            "Failed to resolve dynamic routing key from database, falling back to default", e);
      }
    }
    return getDefaultRoutingKey(featureKey);
  }

  private String getDefaultRoutingKey(String featureKey) {
    if (featureKey != null
        && (featureKey.contains("IMAGE")
            || featureKey.contains("VIDEO")
            || featureKey.contains("MEDIA"))) {
      return "orasaka.routing.media.generate";
    }
    return "orasaka.routing.text.process";
  }

  /**
   * Fallback method triggered when the broker is unreachable or queue is overloaded.
   *
   * @param message the job message
   * @param t the causing throwable
   * @throws SystemOverloadedException the wrapped exception
   */
  public void fallbackQueueFull(JobMessage message, Throwable t) {
    logger.error(
        "Failed to publish job message to queue for job {}. Broker is overloaded or down.",
        message.jobId(),
        t);
    throw new SystemOverloadedException(
        "The system is currently overloaded. Please try again later.", t);
  }

  /**
   * Publishes an approval event for an automation job to the automation exchange.
   *
   * @param jobId The UUID of the approved job.
   * @param userId The user who approved the job.
   */
  public void publishApproval(String jobId, String userId) {
    if (logger.isInfoEnabled()) {
      logger.info("Publishing automation approval for job {}.", sanitize(jobId));
    }
    var approvalMessage =
        Map.of(
            "jobId", jobId,
            "userId", userId,
            "action", "APPROVED");
    rabbitTemplate.convertAndSend("orasaka.automation.exchange", "job.approved", approvalMessage);
  }

  /** Strips CR/LF and control characters to prevent log injection. */
  private static String sanitize(String input) {
    if (input == null) return "null";
    return input.replaceAll("[\\r\\n\\t]", "_");
  }
}
