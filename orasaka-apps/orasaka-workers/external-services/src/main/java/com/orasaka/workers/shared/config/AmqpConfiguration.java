package com.orasaka.workers.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ topology configuration for the external services worker. */
@Configuration
public class AmqpConfiguration {

  // --- Automation Topology ---

  @Bean
  public TopicExchange automationExchange() {
    return new TopicExchange(AmqpConstants.AUTOMATION_EXCHANGE, true, false);
  }

  @Bean
  public Queue automationJobsQueue() {
    return new Queue(AmqpConstants.JOBS_QUEUE, true);
  }

  @Bean
  public Binding automationJobsBinding(
      Queue automationJobsQueue, TopicExchange automationExchange) {
    return BindingBuilder.bind(automationJobsQueue)
        .to(automationExchange)
        .with(AmqpConstants.JOBS_ROUTING_KEY);
  }

  // --- Identity Events Topology ---

  @Bean
  public TopicExchange identityEventsExchange() {
    return new TopicExchange("orasaka.identity.events", true, false);
  }

  @Bean
  public Queue identityRegistrationQueue() {
    return new Queue("orasaka.workers.identity.registration", true);
  }

  @Bean
  public Binding identityRegistrationBinding(
      Queue identityRegistrationQueue, TopicExchange identityEventsExchange) {
    return BindingBuilder.bind(identityRegistrationQueue)
        .to(identityEventsExchange)
        .with("user.registered");
  }

  @Bean
  public Queue identityPasswordQueue() {
    return new Queue("orasaka.workers.identity.password", true);
  }

  @Bean
  public Binding identityPasswordBinding(
      Queue identityPasswordQueue, TopicExchange identityEventsExchange) {
    return BindingBuilder.bind(identityPasswordQueue)
        .to(identityEventsExchange)
        .with("password.reset");
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
