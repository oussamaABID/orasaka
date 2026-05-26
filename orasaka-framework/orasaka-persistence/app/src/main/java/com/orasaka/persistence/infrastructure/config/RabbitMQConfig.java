package com.orasaka.persistence.infrastructure.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Standard Vanilla RabbitMQ Configuration class. Declares the durable queue, direct exchange,
 * routing binding, and JSON message converter for cross-module DTO payloads.
 */
@Configuration
@EnableConfigurationProperties(BrokerProperties.class)
public class RabbitMQConfig {

  public static final String EXCHANGE = "orasaka.jobs.exchange";
  public static final String QUEUE = "orasaka.core.media";
  public static final String ROUTING_KEY = "orasaka.jobs.routingKey";

  public static final String PROGRESS_QUEUE = "orasaka.core.progress";
  public static final String PROGRESS_ROUTING_KEY = "orasaka.progress.routingKey";

  public static final String DLX_EXCHANGE = "orasaka.media.dlx";
  public static final String DLQ = "orasaka.core.media.dlq";
  public static final String DLX_ROUTING_KEY = "media.failed";

  private final BrokerProperties brokerProperties;

  public RabbitMQConfig(BrokerProperties brokerProperties) {
    this.brokerProperties =
        Objects.requireNonNull(brokerProperties, "BrokerProperties cannot be null");
  }

  @Bean
  public Queue jobsQueue() {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("x-max-length", brokerProperties.queue().maxLength());
    String overflow = brokerProperties.queue().overflowStrategy();
    if ("rejectPublish".equals(overflow)) {
      overflow = "reject-publish";
    }
    arguments.put("x-overflow", overflow);
    arguments.put("x-dead-letter-exchange", DLX_EXCHANGE);
    arguments.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
    return new Queue(QUEUE, true, false, false, arguments);
  }

  @Bean
  public Queue progressQueue() {
    return new Queue(PROGRESS_QUEUE, true, false, false);
  }

  @Bean
  public DirectExchange jobsExchange() {
    return new DirectExchange(EXCHANGE);
  }

  @Bean
  public Binding jobsBinding(Queue jobsQueue, DirectExchange jobsExchange) {
    return BindingBuilder.bind(jobsQueue).to(jobsExchange).with(ROUTING_KEY);
  }

  @Bean
  public Binding progressBinding(Queue progressQueue, DirectExchange jobsExchange) {
    return BindingBuilder.bind(progressQueue).to(jobsExchange).with(PROGRESS_ROUTING_KEY);
  }

  @Bean
  public DirectExchange mediaDlxExchange() {
    return new DirectExchange(DLX_EXCHANGE);
  }

  @Bean
  public Queue mediaDlxQueue() {
    return new Queue(DLQ, true, false, false);
  }

  @Bean
  public Binding mediaDlxBinding(Queue mediaDlxQueue, DirectExchange mediaDlxExchange) {
    return BindingBuilder.bind(mediaDlxQueue).to(mediaDlxExchange).with(DLX_ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
