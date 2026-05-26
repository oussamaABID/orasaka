package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.AiMcpServerDto;
import com.orasaka.persistence.identity.domain.ports.AiMcpServerPersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiMcpServerEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.AiMcpServerRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of AiMcpServerPersistenceProvider. */
@Service
@Transactional
class AiMcpServerPersistenceProviderImpl implements AiMcpServerPersistenceProvider {

  private final AiMcpServerRepository repository;

  AiMcpServerPersistenceProviderImpl(AiMcpServerRepository repository) {
    this.repository = Objects.requireNonNull(repository, "AiMcpServerRepository cannot be null");
  }

  @Override
  @Transactional(readOnly = true)
  public List<AiMcpServerDto> findByUserIdAndEnabledTrue(String userId) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    return repository.findByUserIdAndEnabledTrue(userId).stream()
        .map(AiMcpServerPersistenceMapper::toDto)
        .toList();
  }

  @Override
  public AiMcpServerDto save(AiMcpServerDto serverDto) {
    Objects.requireNonNull(serverDto, "AiMcpServerDto cannot be null");
    AiMcpServerEntity entity = AiMcpServerPersistenceMapper.toEntity(serverDto);
    AiMcpServerEntity saved = repository.save(entity);
    return AiMcpServerPersistenceMapper.toDto(saved);
  }

  @Override
  public void deleteById(Integer id) {
    Objects.requireNonNull(id, "ID cannot be null");
    repository.deleteById(id);
  }
}
