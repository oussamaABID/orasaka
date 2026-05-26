package com.orasaka.persistence.identity.application.service;

import com.orasaka.persistence.identity.domain.model.UserProfileDto;
import com.orasaka.persistence.identity.domain.ports.UserProfilePersistenceProvider;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserProfileEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.UserProfileRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of UserProfilePersistenceProvider. */
@Service
@Transactional
class UserProfilePersistenceProviderImpl implements UserProfilePersistenceProvider {

  private final UserProfileRepository userProfileRepository;

  UserProfilePersistenceProviderImpl(UserProfileRepository userProfileRepository) {
    this.userProfileRepository =
        Objects.requireNonNull(userProfileRepository, "UserProfileRepository cannot be null");
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserProfileDto> findByUserId(String userId) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    return userProfileRepository.findById(userId).map(UserProfilePersistenceMapper::toDto);
  }

  @Override
  public UserProfileDto save(UserProfileDto profileDto) {
    Objects.requireNonNull(profileDto, "UserProfileDto cannot be null");
    UserProfileEntity entity = UserProfilePersistenceMapper.toEntity(profileDto);
    UserProfileEntity saved = userProfileRepository.save(entity);
    return UserProfilePersistenceMapper.toDto(saved);
  }
}
