package com.orasaka.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties record for environment-driven Orasaka gateway settings.
 *
 * <p>Maps properties with the {@code orasaka.gateway} prefix from {@code application.yml}
 * dynamically into an immutable Java 21 Record per the system configurations standards.
 *
 * @param cors The CORS properties mapping allowed origins, headers, and credentials.
 * @param security The security properties mapping dev/test bypass options.
 * @see SecurityConfig
 */
@ConfigurationProperties(prefix = "orasaka.gateway")
public record GatewayProperties(CorsProperties cors, SecurityProperties security) {

  /**
   * CORS policy configuration properties.
   *
   * @param allowedOrigins A comma-separated list of browser origins permitted to cross-communicate.
   */
  public record CorsProperties(String allowedOrigins) {}

  /**
   * Security policy configuration properties.
   *
   * @param devBypassId The mock identifier accepted for developer testing.
   * @param devBypassEnabled Flag explicitly enabling the bypass capability.
   */
  public record SecurityProperties(String devBypassId, boolean devBypassEnabled) {}
}
