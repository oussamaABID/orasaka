package com.orasaka.gateway.graphql;

/**
 * Immutable request payload for the {@code POST /api/v1/auth/register} endpoint.
 *
 * <p>Carries the minimal set of fields needed to bootstrap a new Orasaka user account. All fields
 * are required except {@code language}, which defaults to {@code "en"} server-side when absent.
 *
 * @param username The desired display name / login name (must be unique).
 * @param email The user's email address (must be unique).
 * @param password The plaintext password — BCrypt-encoded server-side before persistence.
 * @param language Optional BCP-47 language tag for the preferred UI locale (e.g. {@code "en"},
 *     {@code "fr"}).
 * @see com.orasaka.gateway.rest.ChatStreamController#register(RegisterRequest)
 */
public record RegisterRequest(String username, String email, String password, String language) {
  /**
   * Compact constructor enforcing validation rules for registration requests.
   *
   * @throws com.orasaka.identity.exception.InvalidRequestException if required parameters are
   *     missing or empty.
   */
  public RegisterRequest {
    if (username == null
        || username.isBlank()
        || email == null
        || email.isBlank()
        || password == null
        || password.isBlank()) {
      throw new com.orasaka.identity.exception.InvalidRequestException(
          "username, email, and password are required");
    }
  }
}
