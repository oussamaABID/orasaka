package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.model.PlatformToolConfigDto;
import com.orasaka.persistence.domain.ports.inbound.PlatformToolConfigPersistenceProvider;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PlatformToolConfigEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.PlatformToolConfigRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Package-private implementation of PlatformToolConfigPersistenceProvider. */
@Service
class PlatformToolConfigPersistenceProviderImpl implements PlatformToolConfigPersistenceProvider {

  private final PlatformToolConfigRepository repository;

  PlatformToolConfigPersistenceProviderImpl(PlatformToolConfigRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "PlatformToolConfigRepository cannot be null");
  }

  @Override
  public Optional<PlatformToolConfigDto> findByToolId(String toolId) {
    Objects.requireNonNull(toolId, "toolId cannot be null");
    return repository.findByToolId(toolId).map(PlatformToolConfigMapper::toDto);
  }

  @Override
  public PlatformToolConfigDto save(PlatformToolConfigDto configDto) {
    Objects.requireNonNull(configDto, "PlatformToolConfigDto cannot be null");
    PlatformToolConfigEntity entity = PlatformToolConfigMapper.toEntity(configDto);
    PlatformToolConfigEntity saved = repository.save(entity);
    return PlatformToolConfigMapper.toDto(saved);
  }
}
