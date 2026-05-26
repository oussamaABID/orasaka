package com.orasaka.persistence.identity.domain.ports;

import com.orasaka.persistence.identity.domain.model.UserDto;
import java.util.Optional;

/** Port interface for managing User persistence operations. */
public interface UserPersistenceProvider {

  Optional<UserDto> findById(String id);

  Optional<UserDto> findByEmailAndEnabledTrue(String email);

  Optional<UserDto> findByProviderAndProviderId(String provider, String providerId);

  long countByEmail(String email);

  UserDto save(UserDto userDto);

  UserDto create(UserDto userDto, String passwordHash);

  void deleteById(String id);

  /**
   * Updates the password hash and password_changed_at for a user identified by email.
   *
   * @param email The user's email address.
   * @param passwordHash The new BCrypt password hash.
   */
  void updatePasswordHashByEmail(String email, String passwordHash);

  /**
   * Resolves any user by email, regardless of enabled status.
   *
   * @param email The user's email address.
   * @return An Optional containing the matching UserDto, if found.
   */
  Optional<UserDto> findByEmail(String email);
}
