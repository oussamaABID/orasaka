package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserInterceptionDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserInterceptionEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserInterceptionId;

/** Package-private static mapper for UserInterception entities, satisfying ERR-107. */
final class UserInterceptionPersistenceMapper {

  private UserInterceptionPersistenceMapper() {}

  static UserInterceptionDto toDto(UserInterceptionEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserInterceptionDto(
        entity.getId().getUserId(),
        entity.getId().getInterceptionType(),
        true, // If entity exists, it is active
        entity.getCreatedAt());
  }

  static UserInterceptionEntity toEntity(UserInterceptionDto dto, String schemaId) {
    if (dto == null) {
      return null;
    }
    UserInterceptionEntity entity = new UserInterceptionEntity();
    UserInterceptionId interceptionId =
        new UserInterceptionId(dto.userId(), dto.interceptionType());
    entity.setId(interceptionId);
    entity.setSchemaId(schemaId);
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
