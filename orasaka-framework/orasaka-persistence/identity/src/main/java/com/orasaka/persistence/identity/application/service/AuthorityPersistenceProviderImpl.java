package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.AuthorityDto;
import com.orasaka.persistence.identity.domain.ports.AuthorityPersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AuthorityEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.AuthorityRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of AuthorityPersistenceProvider. */
@Service
@Transactional
class AuthorityPersistenceProviderImpl implements AuthorityPersistenceProvider {

  private final AuthorityRepository repository;

  AuthorityPersistenceProviderImpl(AuthorityRepository repository) {
    this.repository = Objects.requireNonNull(repository, "AuthorityRepository cannot be null");
  }

  @Override
  public void saveAuthority(String userId, String authorityName) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(authorityName, "AuthorityName cannot be null");
    AuthorityEntity entity = new AuthorityEntity();
    entity.setUserId(userId);
    entity.setAuthorityName(authorityName);
    repository.save(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AuthorityDto> findByUserId(String userId) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    return repository.findByUserId(userId).stream().map(AuthorityPersistenceMapper::toDto).toList();
  }
}
