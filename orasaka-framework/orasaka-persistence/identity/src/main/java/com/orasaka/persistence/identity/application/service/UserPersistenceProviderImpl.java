package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserDto;
import com.orasaka.persistence.identity.domain.ports.UserPersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of UserPersistenceProvider. */
@Service
@Transactional
class UserPersistenceProviderImpl implements UserPersistenceProvider {

  private final UserRepository userRepository;
  private static final String EMAIL_CANNOT_BE_NULL = "Email cannot be null";

  UserPersistenceProviderImpl(UserRepository userRepository) {
    this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserDto> findById(String id) {
    Objects.requireNonNull(id, "ID cannot be null");
    return userRepository.findByIdWithAssociations(id).map(UserPersistenceMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserDto> findByEmailAndEnabledTrue(String email) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    return userRepository
        .findByEmailAndEnabledTrueWithAssociations(email)
        .map(UserPersistenceMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserDto> findByProviderAndProviderId(String provider, String providerId) {
    Objects.requireNonNull(provider, "Provider cannot be null");
    Objects.requireNonNull(providerId, "ProviderId cannot be null");
    return userRepository
        .findByProviderAndProviderId(provider, providerId)
        .map(UserPersistenceMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public long countByEmail(String email) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    return userRepository.countByEmail(email);
  }

  @Override
  public UserDto save(UserDto userDto) {
    Objects.requireNonNull(userDto, "UserDto cannot be null");
    UserEntity existing =
        userRepository
            .findByIdWithAssociations(userDto.id())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDto.id()));
    existing.setUsername(userDto.username());
    existing.setEmail(userDto.email());
    existing.setEnabled(userDto.enabled());
    existing.setPreferences(userDto.preferences());
    existing.setRateLimitTier(userDto.rateLimitTier());
    UserEntity saved = userRepository.save(existing);
    return UserPersistenceMapper.toDto(saved);
  }

  @Override
  public UserDto create(UserDto userDto, String passwordHash) {
    Objects.requireNonNull(userDto, "UserDto cannot be null");
    UserEntity entity = UserPersistenceMapper.toEntity(userDto);
    entity.setPasswordHash(passwordHash);
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(Instant.now());
    }
    UserEntity saved = userRepository.save(entity);
    return UserPersistenceMapper.toDto(saved);
  }

  @Override
  public void deleteById(String id) {
    Objects.requireNonNull(id, "ID cannot be null");
    userRepository.deleteById(id);
  }

  @Override
  public void updatePasswordHashByEmail(String email, String passwordHash) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    Objects.requireNonNull(passwordHash, "Password hash cannot be null");
    userRepository.updatePasswordHashByEmail(email, passwordHash);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserDto> findByEmail(String email) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    return userRepository.findByEmail(email).map(UserPersistenceMapper::toDto);
  }
}
