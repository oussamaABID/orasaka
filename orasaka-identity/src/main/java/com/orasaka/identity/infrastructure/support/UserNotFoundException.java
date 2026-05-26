package com.orasaka.identity.infrastructure.support;

/**
 * Thrown when a user lookup fails because no matching record exists.
 *
 * @since 1.0.0
 */
public class UserNotFoundException extends RuntimeException {

  /** Constructs exception with a descriptive message. */
  public UserNotFoundException(String userId) {
    super("User not found: " + userId);
  }
}
