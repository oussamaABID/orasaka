package com.orasaka.automation.infrastructure.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Tests for {@link AmqpConfiguration} bean topology declarations.
 *
 * <p>Validates: exchange is durable and non-auto-delete, queue is durable, binding uses correct
 * routing key, and JSON message converter is configured.
 */
class AmqpConfigurationTest {

  private AmqpConfiguration config;

  @BeforeEach
  void setUp() {
    config = new AmqpConfiguration();
  }

  @Test
  @DisplayName("Should create a durable, non-auto-delete topic exchange")
  void exchangeIsDurableAndNonAutoDelete() {
    TopicExchange exchange = config.automationExchange();

    assertThat(exchange.getName()).isEqualTo(AmqpConstants.AUTOMATION_EXCHANGE);
    assertThat(exchange.isDurable()).isTrue();
    assertThat(exchange.isAutoDelete()).isFalse();
  }

  @Test
  @DisplayName("Should create a durable queue for automation jobs")
  void queueIsDurable() {
    Queue queue = config.automationJobsQueue();

    assertThat(queue.getName()).isEqualTo(AmqpConstants.JOBS_QUEUE);
    assertThat(queue.isDurable()).isTrue();
  }

  @Test
  @DisplayName("Should bind queue to exchange with correct routing key")
  void bindingUsesCorrectRoutingKey() {
    TopicExchange exchange = config.automationExchange();
    Queue queue = config.automationJobsQueue();
    Binding binding = config.automationJobsBinding(queue, exchange);

    assertThat(binding.getExchange()).isEqualTo(AmqpConstants.AUTOMATION_EXCHANGE);
    assertThat(binding.getDestination()).isEqualTo(AmqpConstants.JOBS_QUEUE);
    assertThat(binding.getRoutingKey()).isEqualTo(AmqpConstants.JOBS_ROUTING_KEY);
  }

  @Test
  @DisplayName("Should configure Jackson2JsonMessageConverter for JSON serialization")
  void jsonMessageConverterConfigured() {
    MessageConverter converter = config.jsonMessageConverter();

    assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
  }
}
