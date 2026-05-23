package com.orasaka.gateway.endpoint;

import com.orasaka.identity.domain.User;
import com.orasaka.identity.exception.InvalidRequestException;

/** Group of immutable authentication data contracts. */
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
   * Request data containing OAuth profile details.
   *
   * @param email The user's email address.
   * @param username The user's screen name or display handle.
   */
  public record OAuthRequest(String email, String username) {
    /** Compact constructor validating OAuth email. */
    public OAuthRequest {
      if (email == null || email.isBlank()) {
        throw new InvalidRequestException("Email is required for OAuth login");
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
   * Result of registration indicating either success or failure.
   *
   * @param user The created user.
   * @param error The error message.
   */
  public record RegisterResult(User user, String error) {
    /**
     * Creates a successful result.
     *
     * @param user The created user.
     * @return Success result containing user.
     */
    public static RegisterResult success(User user) {
      return new RegisterResult(user, null);
    }

    /**
     * Creates a failure result.
     *
     * @param error The failure message.
     * @return Failure result containing error.
     */
    public static RegisterResult failure(String error) {
      return new RegisterResult(null, error);
    }
  }
}
