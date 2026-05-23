package com.orasaka.identity.repository;

import com.orasaka.identity.entity.OrasakaUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link OrasakaUserEntity}. */
@Repository
public interface OrasakaUserRepository extends JpaRepository<OrasakaUserEntity, String> {

  /**
   * Resolves an enabled user by their email address.
   *
   * @param email The user email.
   * @return An Optional containing the matched user entity, if any.
   */
  Optional<OrasakaUserEntity> findByEmailAndEnabledTrue(String email);

  /**
   * Counts the number of users registered with the specified email address.
   *
   * @param email The email to check.
   * @return The count of matching users.
   */
  long countByEmail(String email);
}
