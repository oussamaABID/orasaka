package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserMcpServerDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserMcpServerEntity;

/** Package-private static mapper for UserMcpServer entities, satisfying ERR-107. */
final class UserMcpServerMapper {

  private UserMcpServerMapper() {}

  static UserMcpServerDto toDto(UserMcpServerEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserMcpServerDto(
        entity.getId(),
        entity.getUserId(),
        entity.getLabel(),
        entity.getUrl(),
        entity.getAuthToken(),
        entity.getEnabled(),
        entity.getCreatedAt());
  }

  static UserMcpServerEntity toEntity(UserMcpServerDto dto) {
    if (dto == null) {
      return null;
    }
    UserMcpServerEntity entity = new UserMcpServerEntity();
    entity.setId(dto.id());
    entity.setUserId(dto.userId());
    entity.setLabel(dto.label());
    entity.setUrl(dto.url());
    entity.setAuthToken(dto.authToken());
    entity.setEnabled(dto.enabled());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
