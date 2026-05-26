package com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Embeddable composite primary key class for {@link UserInterceptionEntity}. */
@Embeddable
public class UserInterceptionId implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "user_id", length = 255)
  private String userId;

  @Column(name = "interception_type", length = 100)
  private String interceptionType;

  /** Default constructor required by JPA/Hibernate. */
  public UserInterceptionId() {}

  /**
   * Constructs a new composite key instance.
   *
   * @param userId The user ID.
   * @param interceptionType The type of interception.
   */
  public UserInterceptionId(String userId, String interceptionType) {
    this.userId = userId;
    this.interceptionType = interceptionType;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getInterceptionType() {
    return interceptionType;
  }

  public void setInterceptionType(String interceptionType) {
    this.interceptionType = interceptionType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserInterceptionId that = (UserInterceptionId) o;
    return Objects.equals(userId, that.userId)
        && Objects.equals(interceptionType, that.interceptionType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, interceptionType);
  }
}
