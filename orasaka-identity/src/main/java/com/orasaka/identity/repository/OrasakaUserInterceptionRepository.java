package com.orasaka.identity.repository;

import com.orasaka.identity.entity.OrasakaUserInterceptionEntity;
import com.orasaka.identity.entity.OrasakaUserInterceptionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link OrasakaUserInterceptionEntity}. */
@Repository
public interface OrasakaUserInterceptionRepository
    extends JpaRepository<OrasakaUserInterceptionEntity, OrasakaUserInterceptionId> {

  /**
   * Resolves all active interceptions for a given user.
   *
   * @param userId The unique user identifier.
   * @return A list of matching interception entities.
   */
  List<OrasakaUserInterceptionEntity> findByIdUserId(String userId);
}
