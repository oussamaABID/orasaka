package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.AiMcpServerDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiMcpServerEntity;

/** Package-private static mapper for AiMcpServer entities, satisfying ERR-107. */
final class AiMcpServerPersistenceMapper {

  private AiMcpServerPersistenceMapper() {}

  static AiMcpServerDto toDto(AiMcpServerEntity entity) {
    if (entity == null) {
      return null;
    }
    return new AiMcpServerDto(
        entity.getId(),
        entity.getUserId(),
        entity.getName(),
        entity.getUrl(),
        entity.getEnabled(),
        entity.getCreatedAt());
  }

  static AiMcpServerEntity toEntity(AiMcpServerDto dto) {
    if (dto == null) {
      return null;
    }
    AiMcpServerEntity entity = new AiMcpServerEntity();
    entity.setId(dto.id());
    entity.setUserId(dto.userId());
    entity.setName(dto.name());
    entity.setUrl(dto.url());
    entity.setEnabled(dto.enabled());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
