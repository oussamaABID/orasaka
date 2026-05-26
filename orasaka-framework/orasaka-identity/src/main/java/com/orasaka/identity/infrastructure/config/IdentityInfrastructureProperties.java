package com.orasaka.identity.infrastructure.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for passive generic identity workflows in Orasaka. Properties backing
 * the {@code orasaka.identity} configuration namespace. They represent the technical and physical
 * constraints of the Identity hexagon, independent of core orchestrator logic.
 */
@ConfigurationProperties(prefix = "orasaka.identity")
public record IdentityInfrastructureProperties(
    EmailVerification emailVerification, Interceptions interceptions) {

  /** Compact constructor providing null-safe defaults for optional sub-records. */
  public IdentityInfrastructureProperties {
    if (emailVerification == null) {
      emailVerification = new EmailVerification(false);
    }
    if (interceptions == null) {
      interceptions = new Interceptions(false, Map.of());
    }
  }

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
   * @param schemas Mapping of schema ID to its Resource context.
   */
  public record Interceptions(boolean enabled, Map<String, Resource> schemas) {

    /** Compact constructor ensuring schemas is never null. */
    public Interceptions {
      if (schemas == null) {
        schemas = Map.of();
      }
    }
  }
}
