package com.orasaka.tools.repository;

import com.orasaka.tools.entity.OrasakaToolRagSourceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link OrasakaToolRagSourceEntity}. */
@Repository
public interface OrasakaToolRagSourceRepository
    extends JpaRepository<OrasakaToolRagSourceEntity, Long> {

  /**
   * Resolves all non-ingested RAG sources associated with a given tool ID.
   *
   * @param toolId The unique ID of the tool.
   * @return A list of non-ingested source entities.
   */
  List<OrasakaToolRagSourceEntity> findByToolIdAndIngestedFalse(String toolId);
}
