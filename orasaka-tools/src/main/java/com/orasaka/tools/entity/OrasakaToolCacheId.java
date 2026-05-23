package com.orasaka.tools.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Embeddable composite primary key class for {@link OrasakaToolCacheEntity}. */
@Embeddable
public class OrasakaToolCacheId implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "tool_id", length = 255)
  private String toolId;

  @Column(name = "cache_key")
  private String cacheKey;

  /** Default constructor required by JPA/Hibernate. */
  public OrasakaToolCacheId() {}

  /**
   * Constructs a new composite key instance.
   *
   * @param toolId The ID of the tool.
   * @param cacheKey The cache key.
   */
  public OrasakaToolCacheId(String toolId, String cacheKey) {
    this.toolId = toolId;
    this.cacheKey = cacheKey;
  }

  /**
   * Gets the tool identifier.
   *
   * @return The tool ID.
   */
  public String getToolId() {
    return toolId;
  }

  /**
   * Sets the tool identifier.
   *
   * @param toolId The tool ID.
   */
  public void setToolId(String toolId) {
    this.toolId = toolId;
  }

  /**
   * Gets the cache key.
   *
   * @return The cache key.
   */
  public String getCacheKey() {
    return cacheKey;
  }

  /**
   * Sets the cache key.
   *
   * @param cacheKey The cache key.
   */
  public void setCacheKey(String cacheKey) {
    this.cacheKey = cacheKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OrasakaToolCacheId that = (OrasakaToolCacheId) o;
    return Objects.equals(toolId, that.toolId) && Objects.equals(cacheKey, that.cacheKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(toolId, cacheKey);
  }
}
