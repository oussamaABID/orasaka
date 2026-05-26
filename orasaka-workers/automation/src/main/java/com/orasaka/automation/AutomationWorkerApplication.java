package com.orasaka.automation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Isolated Spring Boot application for executing approved automation jobs. Consumes jobs from
 * RabbitMQ, executes them via Apache Camel connectors or CLI agent dispatch, and reports telemetry.
 *
 * <p>This worker maintains its own Flyway migration set and Quartz JDBC store, completely
 * independent from {@code orasaka-persistence-app}.
 *
 * @since 2.0.0
 */
@SpringBootApplication
public class AutomationWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(AutomationWorkerApplication.class, args);
  }
}
