package com.orasaka.identity.repository;

import com.orasaka.identity.entity.UserInterceptionEntity;
import com.orasaka.identity.entity.UserInterceptionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link UserInterceptionEntity}. */
@Repository
public interface UserInterceptionRepository
    extends JpaRepository<UserInterceptionEntity, UserInterceptionId> {

  /**
   * Resolves all active interceptions for a given user.
   *
   * @param userId The unique user identifier.
   * @return A list of matching interception entities.
   */
  List<UserInterceptionEntity> findByIdUserId(String userId);
}
