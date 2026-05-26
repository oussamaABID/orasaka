package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.VerificationTokenDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.VerificationTokenEntity;

/** Package-private static mapper for VerificationToken entities, satisfying ERR-107. */
final class VerificationTokenPersistenceMapper {

  private VerificationTokenPersistenceMapper() {}

  static VerificationTokenDto toDto(VerificationTokenEntity entity) {
    if (entity == null) {
      return null;
    }
    return new VerificationTokenDto(
        entity.getId(),
        entity.getUserId(),
        entity.getTokenType(),
        entity.getTokenHash(),
        entity.getExpiryTimestamp(),
        entity.getUsed(),
        entity.getCreatedAt());
  }

  static VerificationTokenEntity toEntity(VerificationTokenDto dto) {
    if (dto == null) {
      return null;
    }
    VerificationTokenEntity entity = new VerificationTokenEntity();
    entity.setId(dto.id());
    entity.setUserId(dto.userId());
    entity.setTokenType(dto.tokenType());
    entity.setTokenHash(dto.tokenHash());
    entity.setExpiryTimestamp(dto.expiryTimestamp());
    entity.setUsed(dto.used());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
