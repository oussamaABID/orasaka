package com.orasaka.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties record for environment-driven Orasaka gateway settings.
 *
 * <p>Maps properties with the {@code orasaka.gateway} prefix from {@code application.yml}
 * dynamically into an immutable Java 21 Record per the system configurations standards.
 *
 * @param cors The CORS properties mapping allowed origins, headers, and credentials.
 * @see SecurityConfig
 */
@ConfigurationProperties(prefix = "orasaka.gateway")
public record GatewayProperties(CorsProperties cors) {

  /**
   * CORS policy configuration properties.
   *
   * @param allowedOrigins A comma-separated list of browser origins permitted to cross-communicate.
   * @return The CorsProperties record.
   */
  public record CorsProperties(String allowedOrigins) {}
}
