package com.orasaka.persistence.infrastructure.adapter.messaging;

import com.orasaka.identity.domain.model.UserRegisteredEvent;
import com.orasaka.identity.domain.ports.outbound.UserEventPublisher;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/** AMQP adapter for publishing user registration events downstream. */
@Component
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitUserEventAdapter implements UserEventPublisher {

  private static final Logger logger = LoggerFactory.getLogger(RabbitUserEventAdapter.class);
  private static final String EXCHANGE_NAME = "orasaka.identity.events";
  private static final String ROUTING_KEY = "user.registered";

  private final RabbitTemplate rabbitTemplate;

  public RabbitUserEventAdapter(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "RabbitTemplate cannot be null");
  }

  @Override
  public void publish(UserRegisteredEvent event) {
    Objects.requireNonNull(event, "UserRegisteredEvent cannot be null");
    logger.info("Publishing user.registered event downstream for user: {}", event.user().email());
    rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, event);
  }
}
