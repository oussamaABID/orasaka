package com.orasaka.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code orasaka_user_interceptions} database table.
 *
 * <p>Represents a block or check that intercepts a user flow (like onboarding).
 */
@Entity
@Table(name = "orasaka_user_interceptions")
public class UserInterceptionEntity {

  @EmbeddedId private UserInterceptionId id;

  @Column(name = "schema_id", nullable = false, length = 100)
  private String schemaId;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public UserInterceptionEntity() {}

  /**
   * Gets the composite primary key.
   *
   * @return The composite key.
   */
  public UserInterceptionId getId() {
    return id;
  }

  /**
   * Sets the composite primary key.
   *
   * @param id The composite key.
   */
  public void setId(UserInterceptionId id) {
    this.id = id;
  }

  /**
   * Gets the configuration schema ID associated with this interception type.
   *
   * @return The schema ID.
   */
  public String getSchemaId() {
    return schemaId;
  }

  /**
   * Sets the configuration schema ID associated with this interception type.
   *
   * @param schemaId The schema ID.
   */
  public void setSchemaId(String schemaId) {
    this.schemaId = schemaId;
  }

  /**
   * Gets the timestamp when this interception block was created.
   *
   * @return The creation timestamp.
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the timestamp when this interception block was created.
   *
   * @param createdAt The creation timestamp.
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
