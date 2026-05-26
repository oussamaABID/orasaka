package com.orasaka.gateway.domain.model;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
   * <p>The frontend (NextAuth) performs the OAuth2 protocol negotiation and forwards the resulting
   * identity token to the backend for verification and reconciliation.
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

  /**
   * Strongly-typed authentication response DTO for the REST ingress layer.
   *
   * <p>Replaces raw {@code Map.of()} responses with a fail-fast, self-validating record (ERR-106).
   * Domain-blind — contains only primitive gateway-scoped fields.
   *
   * @param token The user's authentication token (UUID string).
   * @param username The authenticated user's display name.
   * @param activeInterceptions List of currently active interception types for the user.
   */
  public record AuthResponse(
      String token,
      String username,
      String email,
      List<String> authorities,
      List<String> activeInterceptions) {
    /** Compact constructor enforcing fail-fast invariants. */
    public AuthResponse {
      Objects.requireNonNull(token, "Auth token is required");
      Objects.requireNonNull(username, "Username is required");
      Objects.requireNonNull(email, "Email is required");
      authorities = authorities != null ? List.copyOf(authorities) : List.of();
      activeInterceptions =
          activeInterceptions != null ? List.copyOf(activeInterceptions) : List.of();
    }
  }

  /**
   * GraphQL-compatible result record for the {@code register} mutation, mapping to the schema's
   * {@code type RegisterResult { user: User, error: String }}.
   *
   * <p>This record is domain-blind — it has zero imports from {@code com.orasaka.identity.domain}.
   * The domain-to-DTO mapping is performed at the controller boundary, not inside this record.
   *
   * @param user The created user descriptor, or {@code null} on failure.
   * @param error The error message, or {@code null} on success.
   */
  public record RegisterResponse(UserDescriptor user, String error) {
    /**
     * Creates a successful registration result from a pre-built descriptor.
     *
     * @param descriptor The user descriptor DTO (already mapped from domain at the boundary).
     * @return Success result containing the user descriptor.
     */
    public static RegisterResponse success(UserDescriptor descriptor) {
      return new RegisterResponse(descriptor, null);
    }

    /**
     * Creates a failure registration result.
     *
     * @param error The failure message.
     * @return Failure result containing the error.
     */
    public static RegisterResponse failure(String error) {
      return new RegisterResponse(null, error);
    }
  }

  /**
   * GraphQL-compatible DTO record mapping exactly to the {@code type User} in the schema.
   *
   * <p>This record is the gateway's projection of a user identity — domain-blind and decoupled from
   * {@code com.orasaka.identity.domain.User}. It is used inside {@link RegisterResponse} for the
   * GraphQL {@code RegisterResult.user} field.
   *
   * @param id The user's unique identifier (UUID string).
   * @param username The user's display name.
   * @param email The user's email address.
   * @param authorities The user's granted authority names.
   * @param preferences The user's preference map.
   */
  public record UserDescriptor(
      String id,
      String username,
      String email,
      List<String> authorities,
      Map<String, Object> preferences) {
    /** Compact constructor enforcing fail-fast invariants. */
    public UserDescriptor {
      Objects.requireNonNull(id, "User ID is required");
      Objects.requireNonNull(username, "Username is required");
      authorities = authorities != null ? List.copyOf(authorities) : List.of();
      preferences = preferences != null ? Map.copyOf(preferences) : Map.of();
    }
  }

  /**
   * Request payload for initiating a password reset.
   *
   * @param email The email address to send the reset token to.
   */
  public record ForgotPasswordRequest(String email) {
    /** Compact constructor validating email. */
    public ForgotPasswordRequest {
      if (email == null || email.isBlank()) {
        throw new InvalidRequestException("Email is required");
      }
    }
  }

  /**
   * Request payload for executing a password reset.
   *
   * @param token The plaintext reset token received by the user.
   * @param newPassword The new password to set.
   */
  public record ResetPasswordRequest(String token, String newPassword) {
    /** Compact constructor validating required fields. */
    public ResetPasswordRequest {
      if (token == null || token.isBlank()) {
        throw new InvalidRequestException("Reset token is required");
      }
      if (newPassword == null || newPassword.isBlank()) {
        throw new InvalidRequestException("New password is required");
      }
    }
  }

  /**
   * Generic message response for endpoints that return informational text.
   *
   * @param message The response message.
   */
  public record GenericMessageResponse(String message) {
    /** Compact constructor enforcing non-null message. */
    public GenericMessageResponse {
      Objects.requireNonNull(message, "Message is required");
    }
  }
}
