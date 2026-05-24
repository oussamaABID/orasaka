package com.orasaka.tools.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code orasaka_tools_cache} database table.
 *
 * <p>Caches tool execution results to prevent redundant calls.
 */
@Entity
@Table(name = "orasaka_tools_cache")
public class ToolCacheEntity {

  @EmbeddedId private ToolCacheId id;

  @Column(name = "cache_value", nullable = false)
  private String cacheValue;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** Default constructor required by JPA/Hibernate. */
  public ToolCacheEntity() {}

  /**
   * Gets the composite primary key.
   *
   * @return The composite key.
   */
  public ToolCacheId getId() {
    return id;
  }

  /**
   * Sets the composite primary key.
   *
   * @param id The composite key.
   */
  public void setId(ToolCacheId id) {
    this.id = id;
  }

  /**
   * Gets the cached value.
   *
   * @return The cached value payload.
   */
  public String getCacheValue() {
    return cacheValue;
  }

  /**
   * Sets the cached value.
   *
   * @param cacheValue The cached value payload.
   */
  public void setCacheValue(String cacheValue) {
    this.cacheValue = cacheValue;
  }

  /**
   * Gets the cache entry creation timestamp.
   *
   * @return The creation timestamp.
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the cache entry creation timestamp.
   *
   * @param createdAt The creation timestamp.
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Gets the cache entry expiration timestamp.
   *
   * @return The expiration timestamp.
   */
  public Instant getExpiresAt() {
    return expiresAt;
  }

  /**
   * Sets the cache entry expiration timestamp.
   *
   * @param expiresAt The expiration timestamp.
   */
  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }
}
