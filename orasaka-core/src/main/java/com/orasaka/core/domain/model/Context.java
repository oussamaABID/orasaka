package com.orasaka.core.domain.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable context for Orasaka AI requests. Carries multi-session and multi-modal preferences.
 *
 * <p>This context is inherently thread-safe and safe for concurrent access via Virtual Threads due
 * to its immutable nature and defensive copying.
 *
 * @param userId The unique identifier of the user making the request.
 * @param conversationId The session identifier for conversation thread state mapping.
 * @param preferences A map of user-specific execution overrides (e.g. TTS voice models).
 * @param authorities The resolved security roles tied to the user session.
 * @see Authority
 */
public record Context(
    String userId,
    String conversationId,
    Map<String, Object> preferences,
    Set<Authority> authorities) {
  public Context {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(conversationId, "conversationId must not be null");
    preferences =
        (preferences != null)
            ? Collections.unmodifiableMap(Map.copyOf(preferences))
            : Collections.emptyMap();
    authorities =
        (authorities != null)
            ? Collections.unmodifiableSet(Set.copyOf(authorities))
            : Collections.emptySet();
  }

  /**
   * Checks if the user session has the specified authority name.
   *
   * <p>This checking operation is thread-safe and non-blocking under Virtual Thread execution
   * contexts, as it queries the immutable local set of user authorities. The input parameter is
   * normalized internally to uppercase and stripped of whitespace.
   *
   * @param authName The name of the authority role to look up.
   * @return {@code true} if the authority is present in the session context, otherwise {@code
   *     false}.
   * @see Authority
   */
  public boolean hasAuthority(String authName) {
    return authorities.stream()
        .anyMatch(auth -> auth.name().equals(authName.toUpperCase().strip()));
  }

  /**
   * Creates an anonymous context for test and internal pipeline use. Satisfies the non-null
   * invariant with safe defaults.
   *
   * @return An anonymous {@link Context} with no preferences or authorities.
   */
  public static Context anonymous() {
    return new Context("anonymous", "none", Map.of(), Set.of());
  }
}
