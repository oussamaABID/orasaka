package com.orasaka.tools.infrastructure.adapter.persistence.repository;

import com.orasaka.tools.infrastructure.adapter.persistence.entity.ToolRagSourceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link ToolRagSourceEntity}. */
@Repository
public interface ToolRagSourceRepository extends JpaRepository<ToolRagSourceEntity, Long> {

  /**
   * Resolves all non-ingested RAG sources associated with a given tool ID.
   *
   * @param toolId The unique ID of the tool.
   * @return A list of non-ingested source entities.
   */
  List<ToolRagSourceEntity> findByToolIdAndIngestedFalse(String toolId);
}
