package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.AiRagStoreDto;
import com.orasaka.persistence.identity.domain.ports.AiRagStorePersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiRagStoreEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.AiRagStoreRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of AiRagStorePersistenceProvider. */
@Service
@Transactional
class AiRagStorePersistenceProviderImpl implements AiRagStorePersistenceProvider {

  private final AiRagStoreRepository repository;

  AiRagStorePersistenceProviderImpl(AiRagStoreRepository repository) {
    this.repository = Objects.requireNonNull(repository, "AiRagStoreRepository cannot be null");
  }

  @Override
  @Transactional(readOnly = true)
  public List<AiRagStoreDto> findByUserIdAndEnabledTrue(String userId) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    return repository.findByUserIdAndEnabledTrue(userId).stream()
        .map(AiRagStorePersistenceMapper::toDto)
        .toList();
  }

  @Override
  public AiRagStoreDto save(AiRagStoreDto storeDto) {
    Objects.requireNonNull(storeDto, "AiRagStoreDto cannot be null");
    AiRagStoreEntity entity = AiRagStorePersistenceMapper.toEntity(storeDto);
    AiRagStoreEntity saved = repository.save(entity);
    return AiRagStorePersistenceMapper.toDto(saved);
  }

  @Override
  public void deleteById(Integer id) {
    Objects.requireNonNull(id, "ID cannot be null");
    repository.deleteById(id);
  }
}
