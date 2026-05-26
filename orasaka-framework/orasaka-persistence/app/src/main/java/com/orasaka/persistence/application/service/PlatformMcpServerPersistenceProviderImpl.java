package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.model.PlatformMcpServerDto;
import com.orasaka.persistence.domain.ports.inbound.PlatformMcpServerPersistenceProvider;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PlatformMcpServerEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.PlatformMcpServerRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Package-private implementation of PlatformMcpServerPersistenceProvider. */
@Service
class PlatformMcpServerPersistenceProviderImpl implements PlatformMcpServerPersistenceProvider {

  private final PlatformMcpServerRepository repository;

  PlatformMcpServerPersistenceProviderImpl(PlatformMcpServerRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "PlatformMcpServerRepository cannot be null");
  }

  @Override
  public List<PlatformMcpServerDto> findByEnabledTrue() {
    return repository.findByEnabledTrue().stream().map(PlatformMcpServerMapper::toDto).toList();
  }

  @Override
  public List<PlatformMcpServerDto> findAll() {
    return repository.findAll().stream().map(PlatformMcpServerMapper::toDto).toList();
  }

  @Override
  public Optional<PlatformMcpServerDto> findById(Integer id) {
    Objects.requireNonNull(id, "id cannot be null");
    return repository.findById(id).map(PlatformMcpServerMapper::toDto);
  }

  @Override
  public PlatformMcpServerDto save(PlatformMcpServerDto serverDto) {
    Objects.requireNonNull(serverDto, "PlatformMcpServerDto cannot be null");
    PlatformMcpServerEntity entity = PlatformMcpServerMapper.toEntity(serverDto);
    PlatformMcpServerEntity saved = repository.save(entity);
    return PlatformMcpServerMapper.toDto(saved);
  }

  @Override
  public void deleteById(Integer id) {
    Objects.requireNonNull(id, "id cannot be null");
    repository.deleteById(id);
  }
}
