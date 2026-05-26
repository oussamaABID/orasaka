package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserProfileDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserProfileEntity;
import java.util.Collections;
import java.util.Map;

/** Package-private static mapper for UserProfile entities, satisfying ERR-107. */
final class UserProfilePersistenceMapper {

  private UserProfilePersistenceMapper() {}

  static UserProfileDto toDto(UserProfileEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserProfileDto(
        entity.getUserId(),
        entity.getTheme(),
        entity.getVoiceModel(),
        entity.getPrimaryIndustry(),
        entity.getAiBehavior(),
        entity.getRawPreferences() != null
            ? Map.copyOf(entity.getRawPreferences())
            : Collections.emptyMap());
  }

  static UserProfileEntity toEntity(UserProfileDto dto) {
    if (dto == null) {
      return null;
    }
    UserProfileEntity entity = new UserProfileEntity();
    entity.setUserId(dto.userId());
    entity.setTheme(dto.theme());
    entity.setVoiceModel(dto.voiceModel());
    entity.setPrimaryIndustry(dto.primaryIndustry());
    entity.setAiBehavior(dto.aiBehavior());
    entity.setRawPreferences(dto.rawPreferences());
    return entity;
  }
}
