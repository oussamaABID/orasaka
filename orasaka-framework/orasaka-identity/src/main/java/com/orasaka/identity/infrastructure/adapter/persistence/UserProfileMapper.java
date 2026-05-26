package com.orasaka.identity.infrastructure.adapter.persistence;

import com.orasaka.identity.domain.model.UserProfile;
import com.orasaka.persistence.identity.domain.model.UserProfileDto;
import java.util.Map;

/**
 * Package-private static mapper isolating UserProfileDto and UserProfile domain mapping
 * boilerplate. Satisfies ERR-107.
 */
final class UserProfileMapper {

  private UserProfileMapper() {}

  /** Maps UserProfileDto to clean domain record. */
  static UserProfile toDomain(UserProfileDto dto) {
    if (dto == null) {
      return null;
    }
    return new UserProfile(
        dto.userId(),
        dto.theme() == null ? "emerald" : dto.theme(),
        dto.voiceModel() == null ? "alloy" : dto.voiceModel(),
        dto.primaryIndustry() == null ? "tech" : dto.primaryIndustry(),
        dto.aiBehavior(),
        dto.rawPreferences() == null ? Map.of() : dto.rawPreferences());
  }

  /** Maps domain UserProfile to UserProfileDto. */
  static UserProfileDto toDto(UserProfile domain) {
    if (domain == null) {
      return null;
    }
    return new UserProfileDto(
        domain.userId(),
        domain.theme(),
        domain.voiceModel(),
        domain.primaryIndustry(),
        domain.aiBehavior(),
        domain.rawPreferences());
  }
}
