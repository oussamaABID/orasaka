package com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository;

import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiMcpServerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for performs database operations on {@link AiMcpServerEntity}. */
@Repository
public interface AiMcpServerRepository extends JpaRepository<AiMcpServerEntity, Integer> {

  List<AiMcpServerEntity> findByUserIdAndEnabledTrue(String userId);
}
