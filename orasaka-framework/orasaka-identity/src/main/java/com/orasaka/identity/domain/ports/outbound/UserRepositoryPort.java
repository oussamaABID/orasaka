package com.orasaka.identity.domain.ports.outbound;

import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserSecurityInfo;
import java.util.Optional;

/**
 * Outbound port defining user persistence operations, isolating services from database entities.
 */
public interface UserRepositoryPort {

  /**
   * Resolves a user by their unique identifier.
   *
   * @param userId The UUID string identifying the user.
   * @return An Optional containing the User record.
   */
  Optional<User> findById(String userId);

  /**
   * Resolves an active enabled user by email.
   *
   * @param email The user email.
   * @return An Optional containing the User record.
   */
  Optional<User> findByEmailAndEnabledTrue(String email);

  /**
   * Resolves a federated user by provider and provider-specific ID.
   *
   * @param provider The provider name (e.g., "google", "github").
   * @param providerId The user's ID within the provider.
   * @return An Optional containing the User record.
   */
  Optional<User> findByProviderAndProviderId(String provider, String providerId);

  /**
   * Resolves a user's security credentials and basic details by email.
   *
   * @param email The user email.
   * @return An Optional containing the UserSecurityInfo credentials record.
   */
  Optional<UserSecurityInfo> findSecurityInfoByEmail(String email);

  /**
   * Saves updates to an existing user's attributes (e.g. preferences).
   *
   * @param user The updated User record.
   * @return The saved User record.
   */
  User save(User user);

  /**
   * Creates a new user record with their initial password hash.
   *
   * @param user The User domain record.
   * @param passwordHash The pre-computed password hash.
   * @return The created User record.
   */
  User create(User user, String passwordHash);

  /**
   * Updates the password hash for a user identified by email.
   *
   * <p>Also updates the {@code password_changed_at} timestamp to enable session invalidation.
   *
   * @param email The user's email address.
   * @param passwordHash The new BCrypt password hash.
   */
  void updatePasswordHashByEmail(String email, String passwordHash);

  /**
   * Resolves any user by email, regardless of enabled status.
   *
   * <p>Used by the password recovery flow to check if an account exists without leaking this
   * information to the caller.
   *
   * @param email The user's email address.
   * @return An Optional containing the User record.
   */
  Optional<User> findByEmail(String email);
}
