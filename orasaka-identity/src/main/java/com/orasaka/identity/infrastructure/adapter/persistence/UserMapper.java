package com.orasaka.identity.infrastructure.adapter.persistence;

import com.orasaka.identity.domain.model.User;
import com.orasaka.persistence.identity.domain.model.UserDto;
import java.util.UUID;

/**
 * Package-private static mapper isolating UserDto and User domain mapping boilerplate. Satisfies
 * ERR-107.
 */
final class UserMapper {

  private UserMapper() {}

  /** Maps UserDto to immutable domain User record. */
  static User toDomain(UserDto dto) {
    if (dto == null) {
      return null;
    }
    return new User(
        UUID.fromString(dto.id()),
        dto.username(),
        dto.email(),
        dto.enabled(),
        dto.authorities(),
        dto.preferences(),
        dto.interceptions(),
        dto.rateLimitTier());
  }

  /** Maps domain User to UserDto. */
  static UserDto toDto(User user, String passwordHash) {
    if (user == null) {
      return null;
    }
    return new UserDto(
        user.id().toString(),
        user.username(),
        passwordHash,
        user.email(),
        user.enabled(),
        user.preferences(),
        user.authorities(),
        user.activeInterceptions(),
        "local",
        null,
        user.rateLimitTier(),
        null);
  }
}
