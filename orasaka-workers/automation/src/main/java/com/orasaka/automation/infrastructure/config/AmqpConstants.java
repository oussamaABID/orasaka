package com.orasaka.automation.infrastructure.config;

/**
 * Centralized AMQP topology constants for the automation worker.
 *
 * @since 2026.1.0
 */
public final class AmqpConstants {

  /** Topic exchange for all automation events. */
  public static final String AUTOMATION_EXCHANGE = "orasaka.automation.exchange";

  /** Durable queue for approved automation jobs. */
  public static final String JOBS_QUEUE = "orasaka.automation.jobs";

  /** Routing key for approved job dispatches. */
  public static final String JOBS_ROUTING_KEY = "job.approved";

  /** Routing key for telemetry and progress updates. */
  public static final String TELEMETRY_ROUTING_KEY = "job.telemetry";

  private AmqpConstants() {}
}
