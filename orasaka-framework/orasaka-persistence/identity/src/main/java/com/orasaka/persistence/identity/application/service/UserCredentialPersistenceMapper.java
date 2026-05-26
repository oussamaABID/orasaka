package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserCredentialDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserCredentialEntity;

/** Package-private static mapper for UserCredential entities, satisfying ERR-107. */
final class UserCredentialPersistenceMapper {

  private UserCredentialPersistenceMapper() {}

  static UserCredentialDto toDto(UserCredentialEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserCredentialDto(
        entity.getId(), entity.getUserId(), entity.getProviderName(), entity.getApiKey());
  }

  static UserCredentialEntity toEntity(UserCredentialDto dto) {
    if (dto == null) {
      return null;
    }
    UserCredentialEntity entity = new UserCredentialEntity();
    entity.setId(dto.id());
    entity.setUserId(dto.userId());
    entity.setProviderName(dto.providerName());
    entity.setApiKey(dto.apiKey());
    return entity;
  }
}
