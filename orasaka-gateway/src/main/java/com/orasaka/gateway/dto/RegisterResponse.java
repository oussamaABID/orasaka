package com.orasaka.gateway.dto;

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
