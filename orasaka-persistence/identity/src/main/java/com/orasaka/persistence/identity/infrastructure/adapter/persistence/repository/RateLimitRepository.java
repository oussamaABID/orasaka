package com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository;

import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.RateLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA Repository for performing database operations on {@link RateLimitEntity}. */
public interface RateLimitRepository extends JpaRepository<RateLimitEntity, String> {}
