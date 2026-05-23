package com.orasaka.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA Entity mapping the {@code orasaka_authorities} database table.
 *
 * <p>Represents a security authority role granted to a user.
 */
@Entity
@Table(
    name = "orasaka_authorities",
    uniqueConstraints =
        @UniqueConstraint(
            name = "unique_user_authority",
            columnNames = {"user_id", "authority_name"}))
public class OrasakaAuthorityEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "user_id", nullable = false, length = 255)
  private String userId;

  @Column(name = "authority_name", nullable = false, length = 100)
  private String authorityName;

  /** Default constructor required by JPA/Hibernate. */
  public OrasakaAuthorityEntity() {}

  /**
   * Gets the generated unique ID.
   *
   * @return The ID.
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the generated unique ID.
   *
   * @param id The ID.
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets the unique identifier of the user this authority is mapped to.
   *
   * @return The user ID.
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the unique identifier of the user this authority is mapped to.
   *
   * @param userId The user ID.
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Gets the role or authority name.
   *
   * @return The authority name.
   */
  public String getAuthorityName() {
    return authorityName;
  }

  /**
   * Sets the role or authority name.
   *
   * @param authorityName The authority name.
   */
  public void setAuthorityName(String authorityName) {
    this.authorityName = authorityName;
  }
}
