package com.orasaka.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Embeddable composite primary key class for {@link OrasakaUserInterceptionEntity}. */
@Embeddable
public class OrasakaUserInterceptionId implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "user_id", length = 255)
  private String userId;

  @Column(name = "interception_type", length = 100)
  private String interceptionType;

  /** Default constructor required by JPA/Hibernate. */
  public OrasakaUserInterceptionId() {}

  /**
   * Constructs a new composite key instance.
   *
   * @param userId The user ID.
   * @param interceptionType The type of interception.
   */
  public OrasakaUserInterceptionId(String userId, String interceptionType) {
    this.userId = userId;
    this.interceptionType = interceptionType;
  }

  /**
   * Gets the user identifier.
   *
   * @return The user ID.
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the user identifier.
   *
   * @param userId The user ID.
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Gets the type of interception.
   *
   * @return The interception type.
   */
  public String getInterceptionType() {
    return interceptionType;
  }

  /**
   * Sets the type of interception.
   *
   * @param interceptionType The interception type.
   */
  public void setInterceptionType(String interceptionType) {
    this.interceptionType = interceptionType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OrasakaUserInterceptionId that = (OrasakaUserInterceptionId) o;
    return Objects.equals(userId, that.userId)
        && Objects.equals(interceptionType, that.interceptionType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, interceptionType);
  }
}
