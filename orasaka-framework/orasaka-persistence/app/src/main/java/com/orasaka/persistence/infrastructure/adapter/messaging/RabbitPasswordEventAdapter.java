package com.orasaka.persistence.infrastructure.adapter.messaging;

import com.orasaka.identity.domain.model.PasswordResetRequestedEvent;
import com.orasaka.identity.domain.ports.outbound.PasswordEventPublisher;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/** AMQP adapter for publishing password reset events downstream. */
@Component
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitPasswordEventAdapter implements PasswordEventPublisher {

  private static final Logger logger = LoggerFactory.getLogger(RabbitPasswordEventAdapter.class);
  private static final String EXCHANGE_NAME = "orasaka.identity.events";
  private static final String ROUTING_KEY = "password.reset";

  private final RabbitTemplate rabbitTemplate;

  public RabbitPasswordEventAdapter(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "RabbitTemplate cannot be null");
  }

  @Override
  public void publish(PasswordResetRequestedEvent event) {
    Objects.requireNonNull(event, "PasswordResetRequestedEvent cannot be null");
    logger.info("Publishing password.reset event downstream for email: {}", event.email());
    rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, event);
  }
}
