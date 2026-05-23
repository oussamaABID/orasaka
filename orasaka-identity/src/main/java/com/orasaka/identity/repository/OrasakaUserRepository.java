package com.orasaka.identity.repository;

import com.orasaka.identity.entity.OrasakaUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
   * Resolves a user profile by unique identifier, left join-fetching their authorities and
   * interceptions.
   *
   * @param id The user ID.
   * @return An Optional containing the matched user entity with fetched associations, if any.
   */
  @Query(
      "SELECT u FROM OrasakaUserEntity u LEFT JOIN FETCH u.authorities LEFT JOIN FETCH u.interceptions WHERE u.id = :id")
  Optional<OrasakaUserEntity> findByIdWithAssociations(@Param("id") String id);

  /**
   * Resolves an enabled user by email, left join-fetching their authorities and interceptions.
   *
   * @param email The user email.
   * @return An Optional containing the matched user entity with fetched associations, if any.
   */
  @Query(
      "SELECT u FROM OrasakaUserEntity u LEFT JOIN FETCH u.authorities LEFT JOIN FETCH u.interceptions WHERE u.email = :email AND u.enabled = true")
  Optional<OrasakaUserEntity> findByEmailAndEnabledTrueWithAssociations(
      @Param("email") String email);

  /**
   * Counts the number of users registered with the specified email address.
   *
   * @param email The email to check.
   * @return The count of matching users.
   */
  long countByEmail(String email);
}
