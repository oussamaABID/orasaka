package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserInterceptionDto;
import com.orasaka.persistence.identity.domain.ports.UserInterceptionPersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserInterceptionEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserInterceptionId;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.UserInterceptionRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of UserInterceptionPersistenceProvider. */
@Service
@Transactional
class UserInterceptionPersistenceProviderImpl implements UserInterceptionPersistenceProvider {

  private final UserInterceptionRepository repository;

  UserInterceptionPersistenceProviderImpl(UserInterceptionRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "UserInterceptionRepository cannot be null");
  }

  @Override
  public UserInterceptionDto save(UserInterceptionDto interceptionDto, String schemaId) {
    Objects.requireNonNull(interceptionDto, "UserInterceptionDto cannot be null");
    UserInterceptionEntity entity =
        UserInterceptionPersistenceMapper.toEntity(interceptionDto, schemaId);
    UserInterceptionEntity saved = repository.save(entity);
    return UserInterceptionPersistenceMapper.toDto(saved);
  }

  @Override
  public void deleteByUserIdAndInterceptionType(String userId, String interceptionType) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(interceptionType, "InterceptionType cannot be null");
    UserInterceptionId id = new UserInterceptionId(userId, interceptionType);
    repository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserInterceptionDto> findByUserIdAndInterceptionType(
      String userId, String interceptionType) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(interceptionType, "InterceptionType cannot be null");
    UserInterceptionId id = new UserInterceptionId(userId, interceptionType);
    return repository.findById(id).map(UserInterceptionPersistenceMapper::toDto);
  }
}
