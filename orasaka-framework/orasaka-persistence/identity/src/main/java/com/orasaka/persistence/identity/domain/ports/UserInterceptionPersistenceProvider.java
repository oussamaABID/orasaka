package com.orasaka.persistence.identity.domain.ports;

import com.orasaka.persistence.identity.domain.model.UserInterceptionDto;
import java.util.Optional;

/** Port interface for managing UserInterception persistence operations. */
public interface UserInterceptionPersistenceProvider {

  UserInterceptionDto save(UserInterceptionDto interceptionDto, String schemaId);

  void deleteByUserIdAndInterceptionType(String userId, String interceptionType);

  Optional<UserInterceptionDto> findByUserIdAndInterceptionType(
      String userId, String interceptionType);
}
