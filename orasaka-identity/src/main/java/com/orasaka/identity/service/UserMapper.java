package com.orasaka.identity.service;

import com.orasaka.identity.domain.ExtractedProfile;
import com.orasaka.identity.entity.UserEntity;
import java.time.Instant;
import java.util.Map;

/**
 * Package-private static mapper isolating {@link UserEntity} construction boilerplate from business
 * logic services.
 *
 * <p>This is a pure, side-effect-free transformation utility. It must never be made public — the
 * mapping responsibility is confined to the identity service package.
 */
final class UserMapper {

  private UserMapper() {}

  /**
   * Maps an {@link ExtractedProfile} (from token verification) to a new {@link UserEntity} ready
   * for persistence.
   *
   * @param profile The verified external provider profile.
   * @param userId The pre-generated user identifier (UUID string).
   * @param provider The identity provider name (e.g., "google", "github").
   * @param rateLimitTier The rate-limit tier to assign (e.g., "free").
   * @return A fully populated, unsaved entity.
   */
  static UserEntity toEntity(
      ExtractedProfile profile, String userId, String provider, String rateLimitTier) {
    UserEntity entity = new UserEntity();
    entity.setId(userId);
    entity.setUsername(profile.name());
    entity.setPasswordHash(null);
    entity.setEmail(profile.email());
    entity.setEnabled(true);
    entity.setPreferences(Map.of("language", "en"));
    entity.setProvider(provider);
    entity.setProviderId(profile.providerId());
    entity.setRateLimitTier(rateLimitTier);
    entity.setCreatedAt(Instant.now());
    return entity;
  }

  /**
   * Maps pre-validated registration inputs to a new {@link UserEntity} for self-service signup.
   *
   * @param userId The pre-generated user identifier (UUID string).
   * @param username The validated display name.
   * @param passwordHash The pre-computed BCrypt password hash.
   * @param email The validated email address.
   * @param enabled Whether the account starts enabled (depends on verification config).
   * @param preferences The initial user preferences map.
   * @return A fully populated, unsaved entity.
   */
  static UserEntity toRegistrationEntity(
      String userId,
      String username,
      String passwordHash,
      String email,
      boolean enabled,
      Map<String, Object> preferences) {
    UserEntity entity = new UserEntity();
    entity.setId(userId);
    entity.setUsername(username);
    entity.setPasswordHash(passwordHash);
    entity.setEmail(email);
    entity.setEnabled(enabled);
    entity.setPreferences(preferences);
    entity.setCreatedAt(Instant.now());
    return entity;
  }
}
