package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserMcpServerDto;
import com.orasaka.persistence.identity.domain.ports.UserMcpServerPersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserMcpServerEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.UserMcpServerRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Package-private implementation of UserMcpServerPersistenceProvider. */
@Service
class UserMcpServerPersistenceProviderImpl implements UserMcpServerPersistenceProvider {

  private final UserMcpServerRepository repository;

  UserMcpServerPersistenceProviderImpl(UserMcpServerRepository repository) {
    this.repository = Objects.requireNonNull(repository, "UserMcpServerRepository cannot be null");
  }

  @Override
  public List<UserMcpServerDto> findByUserIdAndEnabledTrue(String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    return repository.findByUserIdAndEnabledTrue(userId).stream()
        .map(UserMcpServerMapper::toDto)
        .toList();
  }

  @Override
  public List<UserMcpServerDto> findByUserId(String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    return repository.findByUserId(userId).stream().map(UserMcpServerMapper::toDto).toList();
  }

  @Override
  public Optional<UserMcpServerDto> findById(Integer id) {
    Objects.requireNonNull(id, "id cannot be null");
    return repository.findById(id).map(UserMcpServerMapper::toDto);
  }

  @Override
  public UserMcpServerDto save(UserMcpServerDto serverDto) {
    Objects.requireNonNull(serverDto, "UserMcpServerDto cannot be null");
    UserMcpServerEntity entity = UserMcpServerMapper.toEntity(serverDto);
    UserMcpServerEntity saved = repository.save(entity);
    return UserMcpServerMapper.toDto(saved);
  }

  @Override
  public void deleteById(Integer id) {
    Objects.requireNonNull(id, "id cannot be null");
    repository.deleteById(id);
  }
}
