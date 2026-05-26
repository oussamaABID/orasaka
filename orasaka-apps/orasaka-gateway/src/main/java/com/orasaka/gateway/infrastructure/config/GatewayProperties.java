package com.orasaka.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Immutable configuration properties record for the {@code orasaka-gateway} module.
 *
 * <p>Maps properties under the {@code orasaka.gateway} prefix in {@code application.yml}.
 *
 * @param uploads Upload directory, handler path, and cache configuration.
 * @param async Async timeout configuration.
 */
@ConfigurationProperties(prefix = "orasaka.gateway")
public record GatewayProperties(UploadsConfig uploads, AsyncConfig async) {

  /**
   * Upload-related configuration.
   *
   * @param directory Relative path to the upload directory.
   * @param handlerPath URL pattern for serving uploaded files.
   * @param cachePeriod Cache period in seconds for static resources (0 = no cache).
   */
  public record UploadsConfig(String directory, String handlerPath, int cachePeriod) {
    public UploadsConfig {
      if (directory == null || directory.isBlank()) {
        throw new IllegalArgumentException("Upload directory must not be blank");
      }
      if (handlerPath == null || handlerPath.isBlank()) {
        throw new IllegalArgumentException("Upload handler path must not be blank");
      }
    }
  }

  /**
   * Async support configuration.
   *
   * @param timeoutMs Async request timeout in milliseconds.
   */
  public record AsyncConfig(long timeoutMs) {
    public AsyncConfig {
      if (timeoutMs <= 0) {
        throw new IllegalArgumentException("Async timeout must be positive");
      }
    }
  }
}
