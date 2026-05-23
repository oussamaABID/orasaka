package com.orasaka.identity.domain;

import com.orasaka.core.identity.OrasakaAuthority;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable domain representation of an Orasaka user, carrying identity, security authorities, and
 * multi-modal AI execution preferences.
 *
 * <p>This record is thread-safe and compliant with Virtual Thread environments. It is the primary
 * principal type stored in the {@link
 * org.springframework.security.core.context.SecurityContextHolder} by the {@code
 * OrasakaSecurityFilter} in {@code orasaka-gateway}.
 *
 * @param id The universally unique identifier for this user. Generated automatically if not
 *     provided at construction time.
 * @param username The human-readable login name of the user.
 * @param email The verified email address of the user.
 * @param enabled Whether the user account is enabled/active.
 * @param authorities The set of {@link OrasakaAuthority} roles assigned to this user. Defaults to
 *     an empty set if {@code null} is supplied.
 * @param preferences A map of user-specific AI execution overrides (e.g., {@code tts-voice}, {@code
 *     llm-model}). Defaults to an empty map if {@code null} is supplied.
 * @param activeInterceptions A list of active user interception types (e.g., {@code "onboarding"}).
 *     Defaults to an empty list if {@code null} is supplied.
 * @param rateLimitTier The rate limiting tier assigned to this user (e.g., {@code "free"}, {@code
 *     "premium"}).
 * @see com.orasaka.core.identity.OrasakaAuthority
 * @see com.orasaka.core.context.OrasakaContext
 */
public record User(
    UUID id,
    String username,
    String email,
    boolean enabled,
    Set<OrasakaAuthority> authorities,
    Map<String, Object> preferences,
    List<String> activeInterceptions,
    String rateLimitTier) {
  /**
   * Compact canonical constructor that applies defensive defaults.
   *
   * <p>Ensures {@code id} is never {@code null} (generates a random UUID if absent), and coalesces
   * {@code null} collections to empty immutable defaults.
   */
  public User {
    if (id == null) id = UUID.randomUUID();
    if (email == null) email = "";
    if (preferences == null) preferences = Map.of();
    if (authorities == null) authorities = Set.of();
    if (activeInterceptions == null) activeInterceptions = List.of();
  }

  /**
   * Overloaded constructor for backwards compatibility without a rate limiting tier.
   *
   * @param id The universally unique identifier for this user.
   * @param username The human-readable login name of the user.
   * @param email The verified email address of the user.
   * @param enabled Whether the user account is enabled/active.
   * @param authorities The set of {@link OrasakaAuthority} roles assigned to this user.
   * @param preferences A map of user-specific AI execution overrides.
   * @param activeInterceptions A list of active user interception types.
   */
  public User(
      UUID id,
      String username,
      String email,
      boolean enabled,
      Set<OrasakaAuthority> authorities,
      Map<String, Object> preferences,
      List<String> activeInterceptions) {
    this(id, username, email, enabled, authorities, preferences, activeInterceptions, null);
  }
}
