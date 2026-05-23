package com.orasaka.identity.repository;

import com.orasaka.identity.entity.OrasakaRateLimitTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link OrasakaRateLimitTierEntity}. */
@Repository
public interface OrasakaRateLimitTierRepository
    extends JpaRepository<OrasakaRateLimitTierEntity, String> {}
