package com.orasaka.identity.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for passive generic identity workflows in Orasaka. Maps properties under
 * the prefix {@code orasaka.infrastructure.identity}.
 *
 * @param emailVerification Configures the passive generic account verification system.
 * @param interceptions Configures the generic user interception and session resume engine.
 */
@ConfigurationProperties(prefix = "orasaka.infrastructure.identity")
public record IdentityInfrastructureProperties(
    EmailVerification emailVerification, Interceptions interceptions) {
  /**
   * Settings for the email verification subsystem.
   *
   * @param enabled Whether email verification is enabled. If true, newly registered users are
   *     created disabled and must be activated via verification token.
   */
  public record EmailVerification(boolean enabled) {}

  /**
   * Settings for the user interception engine.
   *
   * @param enabled Whether user interceptions are enabled.
   * @param schemas Mapping of schema ID to its file path or URI (e.g.,
   *     classpath:onboarding-schema.json).
   */
  public record Interceptions(boolean enabled, Map<String, String> schemas) {}
}
