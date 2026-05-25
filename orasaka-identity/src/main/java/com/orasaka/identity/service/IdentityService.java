package com.orasaka.identity.service;

import com.orasaka.identity.domain.User;
import java.util.Map;
import java.util.UUID;

/**
 * Port interface defining the identity lifecycle contract consumed by the gateway adapter layer.
 *
 * <p>The gateway binds exclusively to this interface via constructor injection. The concrete
 * implementation ({@code IdentityServiceImpl}) is package-private within {@code orasaka-identity}
 * and discovered by Spring component scanning.
 *
 * <p>Architectural invariants:
 * <ul>
 *   <li>Gateway has zero compile-time awareness of the implementation class [ERR-102]</li>
 *   <li>All methods return domain records, never entities [§2.A Model Separation]</li>
 *   <li>Authentication failures throw explicit exceptions, never return null [ERR-106]</li>
 * </ul>
 *
 * @see User
 */
public interface IdentityService {

  /**
   * Resolves a complete {@link User} profile by their unique identifier.
   *
   * @param userId The UUID string identifying the user.
   * @return The resolved {@link User} record, or {@code null} if no user exists.
   */
  User getUser(String userId);

  /**
   * Authenticates a user by email and password, filtering out disabled accounts.
   *
   * @param email Plaintext email.
   * @param password Plaintext password.
   * @return Fully resolved {@link User} if successful.
   * @throws com.orasaka.identity.exception.BadCredentialsException if authentication fails.
   */
  User authenticate(String email, String password);

  /**
   * Registers a new user account, initiating verification or interceptions if configured.
   *
   * @param username Display name.
   * @param email Email address (must be unique).
   * @param password Plaintext password to hash.
   * @param language Initial language preference.
   * @return Fully resolved {@link User}.
   * @throws com.orasaka.identity.exception.UserAlreadyExistsException if email is taken.
   */
  User register(String username, String email, String password, String language);

  /**
   * Merges preferences map into user preferences.
   *
   * @param userId The user UUID string.
   * @param preferences The preference overrides to merge.
   * @return Fully resolved updated {@link User} record.
   */
  User updatePreferences(String userId, Map<String, Object> preferences);

  /**
   * Resolves an active interception by merging replies and deleting the block row.
   *
   * @param userId The user ID.
   * @param interceptionType The type of interception.
   * @param schemaId The configuration schema ID.
   * @param responses Map of user inputs to merge.
   */
  void resolveInterception(
      UUID userId, String interceptionType, String schemaId, Map<String, Object> responses);

  /**
   * Verifies a verification token and enables the target user account.
   *
   * @param token Plaintext token.
   * @return True if verified successfully; false otherwise.
   */
  boolean verifyToken(String token);
}
