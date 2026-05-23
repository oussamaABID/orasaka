package com.orasaka.identity.entity;

import com.orasaka.identity.entity.converter.JsonMapConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JPA Entity mapping the {@code orasaka_users} database table.
 *
 * <p>Persists core user profile attributes, credentials, and preferences.
 */
@Entity
@Table(name = "orasaka_users")
public class OrasakaUserEntity {

  @Id
  @Column(name = "id", length = 255)
  private String id;

  @Column(name = "username", unique = true, nullable = false, length = 100)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "enabled")
  private Boolean enabled = true;

  @Column(name = "preferences")
  @Convert(converter = JsonMapConverter.class)
  private Map<String, Object> preferences;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
  private Set<OrasakaAuthorityEntity> authorities = new HashSet<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
  private Set<OrasakaUserInterceptionEntity> interceptions = new HashSet<>();

  @Column(name = "rate_limit_tier", length = 50)
  private String rateLimitTier;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public OrasakaUserEntity() {}

  /**
   * Gets the unique user identifier.
   *
   * @return The user ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique user identifier.
   *
   * @param id The user ID.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the username.
   *
   * @return The username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username.
   *
   * @param username The username.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Gets the BCrypt encoded password hash.
   *
   * @return The password hash.
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Sets the BCrypt encoded password hash.
   *
   * @param passwordHash The password hash.
   */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * Gets the email address.
   *
   * @return The email.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email address.
   *
   * @param email The email.
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Checks if the user account is enabled.
   *
   * @return True if enabled, false otherwise.
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether the user account is enabled.
   *
   * @param enabled True to enable, false to disable.
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Gets the user preferences map.
   *
   * @return The preferences map.
   */
  public Map<String, Object> getPreferences() {
    return preferences;
  }

  /**
   * Sets the user preferences map.
   *
   * @param preferences The preferences map.
   */
  public void setPreferences(Map<String, Object> preferences) {
    this.preferences = preferences;
  }

  /**
   * Gets the security authorities entities assigned to the user.
   *
   * @return Set of authority entities.
   */
  public Set<OrasakaAuthorityEntity> getAuthorities() {
    return authorities;
  }

  /**
   * Sets the security authorities entities assigned to the user.
   *
   * @param authorities Set of authority entities.
   */
  public void setAuthorities(Set<OrasakaAuthorityEntity> authorities) {
    this.authorities = authorities;
  }

  /**
   * Gets the user flow interceptions.
   *
   * @return Set of interception entities.
   */
  public Set<OrasakaUserInterceptionEntity> getInterceptions() {
    return interceptions;
  }

  /**
   * Sets the user flow interceptions.
   *
   * @param interceptions Set of interception entities.
   */
  public void setInterceptions(Set<OrasakaUserInterceptionEntity> interceptions) {
    this.interceptions = interceptions;
  }

  /**
   * Gets the identifier of the rate limiting tier.
   *
   * @return The rate limit tier ID.
   */
  public String getRateLimitTier() {
    return rateLimitTier;
  }

  /**
   * Sets the identifier of the rate limiting tier.
   *
   * @param rateLimitTier The rate limit tier ID.
   */
  public void setRateLimitTier(String rateLimitTier) {
    this.rateLimitTier = rateLimitTier;
  }

  /**
   * Gets the user creation timestamp.
   *
   * @return The creation timestamp.
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the user creation timestamp.
   *
   * @param createdAt The creation timestamp.
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
