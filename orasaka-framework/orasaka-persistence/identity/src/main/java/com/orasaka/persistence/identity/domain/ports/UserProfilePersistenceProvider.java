package com.orasaka.persistence.identity.domain.ports;

import com.orasaka.persistence.identity.domain.model.UserProfileDto;
import java.util.Optional;

/** Port interface for managing UserProfile persistence operations. */
public interface UserProfilePersistenceProvider {

  Optional<UserProfileDto> findByUserId(String userId);

  UserProfileDto save(UserProfileDto profileDto);
}
