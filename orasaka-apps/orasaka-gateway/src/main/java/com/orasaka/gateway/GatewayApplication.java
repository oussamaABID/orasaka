package com.orasaka.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Orasaka Gateway Backend-for-Frontend (BFF) application.
 *
 * <p>Configures and runs the Spring Boot web application, setting up base package scanning to
 * automatically discover gateway routes, controllers, and services under the {@code com.orasaka}
 * hierarchy.
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication(scanBasePackages = "com.orasaka")
@ConfigurationPropertiesScan("com.orasaka")
@EnableScheduling
public class GatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
