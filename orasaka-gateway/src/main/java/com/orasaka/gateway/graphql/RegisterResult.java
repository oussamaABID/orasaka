package com.orasaka.gateway.graphql;

import com.orasaka.identity.domain.User;

/**
 * GraphQL projection type for the {@code register} mutation.
 *
 * <p>Follows the discriminated-union pattern: exactly one of {@code user} or {@code error} will be
 * non-null, allowing the client to distinguish a successful registration from a business-level
 * rejection (e.g., duplicate email) without relying on HTTP error codes.
 *
 * @param user The newly created {@link User} on success, or {@code null} on failure.
 * @param error A human-readable error message on failure, or {@code null} on success.
 */
public record RegisterResult(User user, String error) {

  /**
   * Creates a successful registration result containing the user.
   *
   * @param user The created user.
   * @return A successful RegisterResult.
   */
  public static RegisterResult success(User user) {
    return new RegisterResult(user, null);
  }

  /**
   * Creates a failed registration result containing an error message.
   *
   * @param error The failure error message.
   * @return A failed RegisterResult.
   */
  public static RegisterResult failure(String error) {
    return new RegisterResult(null, error);
  }
}
