package com.orasaka.identity.repository;

import com.orasaka.identity.entity.OrasakaAuthorityEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link OrasakaAuthorityEntity}. */
@Repository
public interface OrasakaAuthorityRepository extends JpaRepository<OrasakaAuthorityEntity, Long> {

  /**
   * Finds all authorities configured for a given user.
   *
   * @param userId The unique user identifier.
   * @return A list of matching authority entities.
   */
  List<OrasakaAuthorityEntity> findByUserId(String userId);
}
