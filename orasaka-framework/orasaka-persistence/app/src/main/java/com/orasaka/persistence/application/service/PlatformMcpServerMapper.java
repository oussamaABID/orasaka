package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.model.PlatformMcpServerDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PlatformMcpServerEntity;

/** Package-private static mapper for PlatformMcpServer entities, satisfying ERR-107. */
final class PlatformMcpServerMapper {

  private PlatformMcpServerMapper() {}

  static PlatformMcpServerDto toDto(PlatformMcpServerEntity entity) {
    if (entity == null) {
      return null;
    }
    return new PlatformMcpServerDto(
        entity.getId(),
        entity.getLabel(),
        entity.getTransportType(),
        entity.getUrl(),
        entity.getCommand(),
        entity.getArgs(),
        entity.getAuthToken(),
        entity.getEnabled(),
        entity.getCreatedAt());
  }

  static PlatformMcpServerEntity toEntity(PlatformMcpServerDto dto) {
    if (dto == null) {
      return null;
    }
    PlatformMcpServerEntity entity = new PlatformMcpServerEntity();
    entity.setId(dto.id());
    entity.setLabel(dto.label());
    entity.setTransportType(dto.transportType());
    entity.setUrl(dto.url());
    entity.setCommand(dto.command());
    entity.setArgs(dto.args());
    entity.setAuthToken(dto.authToken());
    entity.setEnabled(dto.enabled());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
