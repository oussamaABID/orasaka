package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.PasswordResetTokenDto;
import com.orasaka.persistence.identity.domain.ports.PasswordResetTokenPersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.PasswordResetTokenEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.PasswordResetTokenJpaRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of PasswordResetTokenPersistenceProvider. */
@Service
@Transactional
class PasswordResetTokenPersistenceProviderImpl implements PasswordResetTokenPersistenceProvider {

  private final PasswordResetTokenJpaRepository repository;

  PasswordResetTokenPersistenceProviderImpl(PasswordResetTokenJpaRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "PasswordResetTokenJpaRepository cannot be null");
  }

  @Override
  public void save(PasswordResetTokenDto dto) {
    Objects.requireNonNull(dto, "PasswordResetTokenDto cannot be null");
    PasswordResetTokenEntity entity = PasswordResetTokenPersistenceMapper.toEntity(dto);
    repository.save(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PasswordResetTokenDto> findByTokenHash(String tokenHash) {
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    return repository.findByTokenHash(tokenHash).map(PasswordResetTokenPersistenceMapper::toDto);
  }

  @Override
  public void deleteById(String id) {
    Objects.requireNonNull(id, "Token ID cannot be null");
    repository.deleteById(id);
  }

  @Override
  public void deleteByEmail(String email) {
    Objects.requireNonNull(email, "Email cannot be null");
    repository.deleteByEmail(email);
  }
}
