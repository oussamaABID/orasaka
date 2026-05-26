package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AuthorityEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserEntity;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Package-private static mapper for User entities, satisfying ERR-107. */
final class UserPersistenceMapper {

  private UserPersistenceMapper() {}

  static UserDto toDto(UserEntity entity) {
    if (entity == null) {
      return null;
    }

    Set<String> authorities =
        entity.getAuthorities() != null
            ? entity.getAuthorities().stream()
                .map(AuthorityEntity::getAuthorityName)
                .collect(Collectors.toUnmodifiableSet())
            : Collections.emptySet();

    List<String> interceptions = Collections.emptyList();
    if (entity.getInterceptions() != null) {
      interceptions =
          entity.getInterceptions().stream().map(i -> i.getId().getInterceptionType()).toList();
    }

    return new UserDto(
        entity.getId(),
        entity.getUsername(),
        entity.getPasswordHash(),
        entity.getEmail(),
        entity.getEnabled(),
        entity.getPreferences() != null
            ? Map.copyOf(entity.getPreferences())
            : Collections.emptyMap(),
        authorities,
        interceptions,
        entity.getProvider(),
        entity.getProviderId(),
        entity.getRateLimitTier(),
        entity.getCreatedAt());
  }

  static UserEntity toEntity(UserDto dto) {
    if (dto == null) {
      return null;
    }

    UserEntity entity = new UserEntity();
    entity.setId(dto.id());
    entity.setUsername(dto.username());
    entity.setPasswordHash(dto.passwordHash());
    entity.setEmail(dto.email());
    entity.setEnabled(dto.enabled());
    entity.setPreferences(dto.preferences());
    entity.setProvider(dto.provider());
    entity.setProviderId(dto.providerId());
    entity.setRateLimitTier(dto.rateLimitTier());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
