package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.RateLimitDto;
import com.orasaka.persistence.identity.domain.ports.RateLimitPersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.RateLimitEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.RateLimitRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of RateLimitPersistenceProvider. */
@Service
@Transactional
class RateLimitPersistenceProviderImpl implements RateLimitPersistenceProvider {

  private final RateLimitRepository repository;

  RateLimitPersistenceProviderImpl(RateLimitRepository repository) {
    this.repository = Objects.requireNonNull(repository, "RateLimitRepository cannot be null");
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<RateLimitDto> findById(String tierKey) {
    Objects.requireNonNull(tierKey, "Tier key cannot be null");
    return repository.findById(tierKey).map(RateLimitPersistenceMapper::toDto);
  }

  @Override
  public RateLimitDto save(RateLimitDto rateLimitDto) {
    Objects.requireNonNull(rateLimitDto, "RateLimitDto cannot be null");
    RateLimitEntity entity = RateLimitPersistenceMapper.toEntity(rateLimitDto);
    RateLimitEntity saved = repository.save(entity);
    return RateLimitPersistenceMapper.toDto(saved);
  }
}
