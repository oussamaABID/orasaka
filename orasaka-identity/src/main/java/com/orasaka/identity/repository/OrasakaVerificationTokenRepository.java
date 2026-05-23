package com.orasaka.identity.repository;

import com.orasaka.identity.entity.OrasakaVerificationTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link OrasakaVerificationTokenEntity}. */
@Repository
public interface OrasakaVerificationTokenRepository
    extends JpaRepository<OrasakaVerificationTokenEntity, String> {

  /**
   * Resolves an unused verification token by its hash.
   *
   * @param tokenHash The hashed verification token value.
   * @return An Optional containing the matching token entity, if any.
   */
  Optional<OrasakaVerificationTokenEntity> findByTokenHashAndUsedFalse(String tokenHash);
}
