package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.PasswordResetTokenDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.PasswordResetTokenEntity;
import java.time.Instant;

/**
 * Package-private static mapper for password reset token entity/DTO conversions (ERR-107).
 *
 * <p>All mapping logic is isolated here — controllers, services, and engines never perform
 * field-by-field mapping.
 */
final class PasswordResetTokenPersistenceMapper {

  private PasswordResetTokenPersistenceMapper() {}

  /**
   * Converts a persistence DTO to a JPA entity.
   *
   * @param dto The persistence DTO.
   * @return The JPA entity.
   */
  static PasswordResetTokenEntity toEntity(PasswordResetTokenDto dto) {
    PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
    entity.setId(dto.id());
    entity.setEmail(dto.email());
    entity.setTokenHash(dto.tokenHash());
    entity.setExpiresAt(dto.expiresAt());
    entity.setCreatedAt(dto.createdAt() != null ? dto.createdAt() : Instant.now());
    return entity;
  }

  /**
   * Converts a JPA entity to a persistence DTO.
   *
   * @param entity The JPA entity.
   * @return The persistence DTO.
   */
  static PasswordResetTokenDto toDto(PasswordResetTokenEntity entity) {
    return new PasswordResetTokenDto(
        entity.getId(),
        entity.getEmail(),
        entity.getTokenHash(),
        entity.getExpiresAt(),
        entity.getCreatedAt());
  }
}
