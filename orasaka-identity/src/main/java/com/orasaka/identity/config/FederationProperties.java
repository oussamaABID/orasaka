package com.orasaka.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties governing the identity federation layer.
 *
 * <p>Maps properties under the prefix {@code orasaka.identity.auth}, controlling which
 * authentication strategies are active at runtime:
 * <ul>
 *   <li>{@code local} — Traditional email/password credential authentication.</li>
 *   <li>{@code oauth2} — External identity providers (Google, GitHub, etc.).</li>
 * </ul>
 *
 * <p>All sensitive values (client IDs, secrets) must be injected via environment variables.
 * Hardcoded credentials are strictly forbidden per AGENTS.md security standards.
 *
 * @param local Configuration for local credential-based authentication.
 * @param oauth2 Configuration for external OAuth2 identity providers.
 */
@ConfigurationProperties(prefix = "orasaka.infrastructure.identity.auth")
public record FederationProperties(LocalAuth local, OAuth2Auth oauth2) {

  /**
   * Settings for the local credential-based authentication strategy.
   *
   * @param enabled Whether local email/password authentication is active.
   */
  public record LocalAuth(boolean enabled) {}

  /**
   * Container for all external OAuth2 provider configurations.
   *
   * @param google Configuration for the Google identity provider.
   * @param github Configuration for the GitHub identity provider.
   */
  public record OAuth2Auth(GoogleConfig google, GithubConfig github) {}

  /**
   * Configuration for the Google OAuth2 identity provider.
   *
   * @param enabled Whether Google authentication is active.
   * @param clientId The Google OAuth2 client identifier, injected via environment variable.
   */
  public record GoogleConfig(boolean enabled, String clientId) {}

  /**
   * Configuration for the GitHub OAuth2 identity provider.
   *
   * @param enabled Whether GitHub authentication is active.
   * @param clientId The GitHub OAuth2 client identifier, injected via environment variable.
   */
  public record GithubConfig(boolean enabled, String clientId) {}
}
