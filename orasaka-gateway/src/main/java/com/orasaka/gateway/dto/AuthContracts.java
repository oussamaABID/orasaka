package com.orasaka.gateway.dto;

import com.orasaka.identity.exception.InvalidRequestException;

/**
 * Group of immutable authentication data contracts for the REST ingress layer.
 *
 * <p>All records use compact constructors for fail-fast validation (ERR-106). This container is
 * domain-blind — it must never import {@code com.orasaka.identity.domain.*}.
 */
public final class AuthContracts {

  private AuthContracts() {}

  /**
   * Request data containing credentials.
   *
   * @param email The user's email address.
   * @param password The user's plaintext password.
   */
  public record LoginRequest(String email, String password) {
    /** Compact constructor validating credentials. */
    public LoginRequest {
      if (email == null || email.isBlank() || password == null || password.isBlank()) {
        throw new InvalidRequestException("Email and password are required");
      }
    }
  }

  /**
   * Request data containing an external OAuth2 identity token for the Token-Exchange pattern.
   *
   * <p>The frontend (NextAuth) performs the OAuth2 protocol negotiation and forwards the
   * resulting identity token to the backend for verification and reconciliation.
   *
   * @param provider The identity provider identifier (e.g., "google", "github").
   * @param idToken The raw identity token or access token issued by the external provider.
   * @param email Optional email hint for logging (actual email is extracted from the token).
   * @param username Optional username hint (fallback to provider profile name if blank).
   */
  public record OAuthRequest(String provider, String idToken, String email, String username) {
    /** Compact constructor validating required fields. */
    public OAuthRequest {
      if (provider == null || provider.isBlank()) {
        throw new InvalidRequestException("Provider is required for OAuth login");
      }
      if (idToken == null || idToken.isBlank()) {
        throw new InvalidRequestException("Identity token is required for OAuth login");
      }
    }
  }

  /**
   * Request payload for verifying account tokens.
   *
   * @param token The token string.
   */
  public record VerifyTokenRequest(String token) {
    /** Compact constructor validating token. */
    public VerifyTokenRequest {
      if (token == null || token.isBlank()) {
        throw new InvalidRequestException("Token is required");
      }
    }
  }

  /**
   * Request data containing registration details.
   *
   * @param username The user's username.
   * @param email The user's email address.
   * @param password The user's password.
   * @param language The user's preferred language.
   */
  public record RegisterRequest(String username, String email, String password, String language) {
    /** Compact constructor validating required fields. */
    public RegisterRequest {
      if (username == null
          || username.isBlank()
          || email == null
          || email.isBlank()
          || password == null
          || password.isBlank()) {
        throw new InvalidRequestException("username, email, and password are required");
      }
    }
  }
}
