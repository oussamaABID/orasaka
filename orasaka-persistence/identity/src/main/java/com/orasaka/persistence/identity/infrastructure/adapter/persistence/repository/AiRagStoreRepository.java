package com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository;

import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiRagStoreEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for performs database operations on {@link AiRagStoreEntity}. */
@Repository
public interface AiRagStoreRepository extends JpaRepository<AiRagStoreEntity, Integer> {

  List<AiRagStoreEntity> findByUserIdAndEnabledTrue(String userId);
}
