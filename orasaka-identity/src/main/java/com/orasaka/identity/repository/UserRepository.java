package com.orasaka.identity.repository;

import com.orasaka.identity.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link UserEntity}. */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

  /**
   * Resolves an enabled user by their email address.
   *
   * @param email The user email.
   * @return An Optional containing the matched user entity, if any.
   */
  Optional<UserEntity> findByEmailAndEnabledTrue(String email);

  /**
   * Resolves a user profile by unique identifier, left join-fetching their authorities and
   * interceptions.
   *
   * @param id The user ID.
   * @return An Optional containing the matched user entity with fetched associations, if any.
   */
  @Query(
      "SELECT u FROM UserEntity u LEFT JOIN FETCH u.authorities LEFT JOIN FETCH u.interceptions WHERE u.id = :id")
  Optional<UserEntity> findByIdWithAssociations(@Param("id") String id);

  /**
   * Resolves an enabled user by email, left join-fetching their authorities and interceptions.
   *
   * @param email The user email.
   * @return An Optional containing the matched user entity with fetched associations, if any.
   */
  @Query(
      "SELECT u FROM UserEntity u LEFT JOIN FETCH u.authorities LEFT JOIN FETCH u.interceptions WHERE u.email = :email AND u.enabled = true")
  Optional<UserEntity> findByEmailAndEnabledTrueWithAssociations(@Param("email") String email);

  /**
   * Counts the number of users registered with the specified email address.
   *
   * @param email The email to check.
   * @return The count of matching users.
   */
  long countByEmail(String email);

  /**
   * Resolves a user by their external identity provider and provider-specific user identifier.
   *
   * @param provider The authentication provider name (e.g., "google", "github").
   * @param providerId The unique user identifier assigned by the external provider.
   * @return An Optional containing the matched user entity, if any.
   */
  Optional<UserEntity> findByProviderAndProviderId(String provider, String providerId);
}
