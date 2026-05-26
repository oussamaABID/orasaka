package com.orasaka.automation.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology configuration for the automation worker.
 *
 * <p>Declares the exchange, queues, and bindings for the automation job pipeline.
 *
 * @since 2.0.0
 */
@Configuration
class AmqpConfiguration {

  @Bean
  TopicExchange automationExchange() {
    return new TopicExchange(AmqpConstants.AUTOMATION_EXCHANGE, true, false);
  }

  @Bean
  Queue automationJobsQueue() {
    return new Queue(AmqpConstants.JOBS_QUEUE, true);
  }

  @Bean
  Binding automationJobsBinding(Queue automationJobsQueue, TopicExchange automationExchange) {
    return BindingBuilder.bind(automationJobsQueue)
        .to(automationExchange)
        .with(AmqpConstants.JOBS_ROUTING_KEY);
  }

  @Bean
  MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
