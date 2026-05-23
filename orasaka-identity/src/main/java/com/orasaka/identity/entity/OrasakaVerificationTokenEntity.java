package com.orasaka.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code orasaka_verification_tokens} database table.
 *
 * <p>Represents a temporary token hash generated for authentication, registration, or password
 * reset flows.
 */
@Entity
@Table(name = "orasaka_verification_tokens")
public class OrasakaVerificationTokenEntity {

  @Id
  @Column(name = "id", length = 255)
  private String id;

  @Column(name = "user_id", nullable = false, length = 255)
  private String userId;

  @Column(name = "token_type", nullable = false, length = 100)
  private String tokenType;

  @Column(name = "token_hash", nullable = false, length = 255)
  private String tokenHash;

  @Column(name = "expiry_timestamp", nullable = false)
  private Instant expiryTimestamp;

  @Column(name = "used")
  private Boolean used = false;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public OrasakaVerificationTokenEntity() {}

  /**
   * Gets the unique token identifier.
   *
   * @return The token ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique token identifier.
   *
   * @param id The token ID.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the unique identifier of the user linked to this token.
   *
   * @return The user ID.
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the unique identifier of the user linked to this token.
   *
   * @param userId The user ID.
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Gets the type category of this token.
   *
   * @return The token type.
   */
  public String getTokenType() {
    return tokenType;
  }

  /**
   * Sets the type category of this token.
   *
   * @param tokenType The token type.
   */
  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  /**
   * Gets the computed secure SHA-256 hash of the token.
   *
   * @return The token hash.
   */
  public String getTokenHash() {
    return tokenHash;
  }

  /**
   * Sets the computed secure SHA-256 hash of the token.
   *
   * @param tokenHash The token hash.
   */
  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  /**
   * Gets the expiration timestamp bounds.
   *
   * @return The expiry timestamp.
   */
  public Instant getExpiryTimestamp() {
    return expiryTimestamp;
  }

  /**
   * Sets the expiration timestamp bounds.
   *
   * @param expiryTimestamp The expiry timestamp.
   */
  public void setExpiryTimestamp(Instant expiryTimestamp) {
    this.expiryTimestamp = expiryTimestamp;
  }

  /**
   * Checks if the token has been consumed.
   *
   * @return True if used, false otherwise.
   */
  public Boolean getUsed() {
    return used;
  }

  /**
   * Sets whether this token has been consumed.
   *
   * @param used True if used, false otherwise.
   */
  public void setUsed(Boolean used) {
    this.used = used;
  }

  /**
   * Gets the token creation timestamp.
   *
   * @return The creation timestamp.
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the token creation timestamp.
   *
   * @param createdAt The creation timestamp.
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
