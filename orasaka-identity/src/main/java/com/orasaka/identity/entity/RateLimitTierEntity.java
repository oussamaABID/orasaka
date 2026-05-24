package com.orasaka.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code orasaka_rate_limit_tiers} database table.
 *
 * <p>Represents a configured rate limiting tier that dictates bucket capacity, refill quotas, and
 * timing bounds.
 */
@Entity
@Table(name = "orasaka_rate_limit_tiers")
public class RateLimitTierEntity {

  @Id
  @Column(name = "id", length = 50)
  private String id;

  @Column(name = "capacity", nullable = false)
  private int capacity;

  @Column(name = "refill_tokens", nullable = false)
  private int refillTokens;

  @Column(name = "refill_seconds", nullable = false)
  private int refillSeconds;

  @Column(name = "updated_at")
  private Instant updatedAt;

  /** Default constructor required by JPA/Hibernate. */
  public RateLimitTierEntity() {}

  /**
   * Gets the unique identifier of the rate limiting tier.
   *
   * @return The tier ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique identifier of the rate limiting tier.
   *
   * @param id The tier ID.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the capacity configuration.
   *
   * @return The capacity limit.
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Sets the capacity configuration.
   *
   * @param capacity The capacity limit.
   */
  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  /**
   * Gets the refill tokens rate limit value.
   *
   * @return The refill tokens amount.
   */
  public int getRefillTokens() {
    return refillTokens;
  }

  /**
   * Sets the refill tokens rate limit value.
   *
   * @param refillTokens The refill tokens amount.
   */
  public void setRefillTokens(int refillTokens) {
    this.refillTokens = refillTokens;
  }

  /**
   * Gets the duration of a refill interval in seconds.
   *
   * @return The refill interval seconds.
   */
  public int getRefillSeconds() {
    return refillSeconds;
  }

  /**
   * Sets the duration of a refill interval in seconds.
   *
   * @param refillSeconds The refill interval seconds.
   */
  public void setRefillSeconds(int refillSeconds) {
    this.refillSeconds = refillSeconds;
  }

  /**
   * Gets the timestamp when this tier configuration was last modified.
   *
   * @return The last update timestamp.
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Sets the timestamp when this tier configuration was last modified.
   *
   * @param updatedAt The last update timestamp.
   */
  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
