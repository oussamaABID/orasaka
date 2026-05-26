package com.orasaka.workers.shared.config;

/** Centralized AMQP topology constants for the external services worker. */
public final class AmqpConstants {

  public static final String AUTOMATION_EXCHANGE = "orasaka.automation.exchange";
  public static final String JOBS_QUEUE = "orasaka.automation.jobs";
  public static final String JOBS_ROUTING_KEY = "job.approved";
  public static final String TELEMETRY_ROUTING_KEY = "job.telemetry";

  private AmqpConstants() {}
}
