package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.AiRagStoreDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiRagStoreEntity;

/** Package-private static mapper for AiRagStore entities, satisfying ERR-107. */
final class AiRagStorePersistenceMapper {

  private AiRagStorePersistenceMapper() {}

  static AiRagStoreDto toDto(AiRagStoreEntity entity) {
    if (entity == null) {
      return null;
    }
    return new AiRagStoreDto(
        entity.getId(),
        entity.getUserId(),
        entity.getName(),
        entity.getStoreType(),
        entity.getHost(),
        entity.getPort(),
        entity.getDatabaseName(),
        entity.getTableName(),
        entity.getUsername(),
        entity.getPassword(),
        entity.getEnabled(),
        entity.getCreatedAt());
  }

  static AiRagStoreEntity toEntity(AiRagStoreDto dto) {
    if (dto == null) {
      return null;
    }
    AiRagStoreEntity entity = new AiRagStoreEntity();
    entity.setId(dto.id());
    entity.setUserId(dto.userId());
    entity.setName(dto.name());
    entity.setStoreType(dto.storeType());
    entity.setHost(dto.host());
    entity.setPort(dto.port());
    entity.setDatabaseName(dto.databaseName());
    entity.setTableName(dto.tableName());
    entity.setUsername(dto.username());
    entity.setPassword(dto.password());
    entity.setEnabled(dto.enabled());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
