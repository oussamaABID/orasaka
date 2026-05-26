package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.VerificationTokenDto;
import com.orasaka.persistence.identity.domain.ports.VerificationTokenPersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.VerificationTokenEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.VerificationTokenRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of VerificationTokenPersistenceProvider. */
@Service
@Transactional
class VerificationTokenPersistenceProviderImpl implements VerificationTokenPersistenceProvider {

  private final VerificationTokenRepository repository;

  VerificationTokenPersistenceProviderImpl(VerificationTokenRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "VerificationTokenRepository cannot be null");
  }

  @Override
  public VerificationTokenDto save(VerificationTokenDto tokenDto) {
    Objects.requireNonNull(tokenDto, "VerificationTokenDto cannot be null");
    VerificationTokenEntity entity = VerificationTokenPersistenceMapper.toEntity(tokenDto);
    VerificationTokenEntity saved = repository.save(entity);
    return VerificationTokenPersistenceMapper.toDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<VerificationTokenDto> findByTokenHashAndUsedFalse(String tokenHash) {
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    return repository
        .findByTokenHashAndUsedFalse(tokenHash)
        .map(VerificationTokenPersistenceMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<VerificationTokenDto> findById(String tokenId) {
    Objects.requireNonNull(tokenId, "Token ID cannot be null");
    return repository.findById(tokenId).map(VerificationTokenPersistenceMapper::toDto);
  }

  @Override
  public void deleteById(String tokenId) {
    Objects.requireNonNull(tokenId, "Token ID cannot be null");
    repository.deleteById(tokenId);
  }
}
