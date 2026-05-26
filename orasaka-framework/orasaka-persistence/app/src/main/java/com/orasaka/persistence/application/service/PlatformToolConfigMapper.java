package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.model.PlatformToolConfigDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PlatformToolConfigEntity;

/** Package-private static mapper for PlatformToolConfig entities, satisfying ERR-107. */
final class PlatformToolConfigMapper {

  private PlatformToolConfigMapper() {}

  static PlatformToolConfigDto toDto(PlatformToolConfigEntity entity) {
    if (entity == null) {
      return null;
    }
    return new PlatformToolConfigDto(
        entity.getId(),
        entity.getToolId(),
        entity.getCacheEnabled(),
        entity.getCacheTtlSeconds(),
        entity.getRagEnabled(),
        entity.getChunkerType(),
        entity.getSourceTable(),
        entity.getCreatedAt());
  }

  static PlatformToolConfigEntity toEntity(PlatformToolConfigDto dto) {
    if (dto == null) {
      return null;
    }
    PlatformToolConfigEntity entity = new PlatformToolConfigEntity();
    entity.setId(dto.id());
    entity.setToolId(dto.toolId());
    entity.setCacheEnabled(dto.cacheEnabled());
    entity.setCacheTtlSeconds(dto.cacheTtlSeconds());
    entity.setRagEnabled(dto.ragEnabled());
    entity.setChunkerType(dto.chunkerType());
    entity.setSourceTable(dto.sourceTable());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
