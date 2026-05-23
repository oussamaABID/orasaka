package com.orasaka.tools.repository;

import com.orasaka.tools.entity.OrasakaToolCacheEntity;
import com.orasaka.tools.entity.OrasakaToolCacheId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link OrasakaToolCacheEntity}. */
@Repository
public interface OrasakaToolCacheRepository
    extends JpaRepository<OrasakaToolCacheEntity, OrasakaToolCacheId> {

  /**
   * Resolves a cached tool execution result by tool ID and cache key.
   *
   * @param toolId The unique ID of the tool.
   * @param cacheKey The key identifier for the cached execution argument.
   * @return An Optional containing the cache entity, if any.
   */
  Optional<OrasakaToolCacheEntity> findByIdToolIdAndIdCacheKey(String toolId, String cacheKey);

  /**
   * Deletes a cached entry by tool ID and cache key.
   *
   * @param toolId The unique ID of the tool.
   * @param cacheKey The key identifier for the cached execution argument.
   */
  void deleteByIdToolIdAndIdCacheKey(String toolId, String cacheKey);
}
