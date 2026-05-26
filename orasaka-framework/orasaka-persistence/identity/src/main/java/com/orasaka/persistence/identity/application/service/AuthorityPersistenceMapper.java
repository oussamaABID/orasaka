package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.AuthorityDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AuthorityEntity;

/** Package-private static mapper for Authority entities, satisfying ERR-107. */
final class AuthorityPersistenceMapper {

  private AuthorityPersistenceMapper() {}

  static AuthorityDto toDto(AuthorityEntity entity) {
    if (entity == null) {
      return null;
    }
    return new AuthorityDto(entity.getId(), entity.getUserId(), entity.getAuthorityName());
  }

  static AuthorityEntity toEntity(AuthorityDto dto) {
    if (dto == null) {
      return null;
    }
    AuthorityEntity entity = new AuthorityEntity();
    entity.setId(dto.id());
    entity.setUserId(dto.userId());
    entity.setAuthorityName(dto.authorityName());
    return entity;
  }
}
