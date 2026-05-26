package com.orasaka.persistence.infrastructure.adapter.persistence.repository;

import com.orasaka.persistence.infrastructure.adapter.persistence.entity.FeatureFlagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link FeatureFlagEntity}. */
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, String> {}
