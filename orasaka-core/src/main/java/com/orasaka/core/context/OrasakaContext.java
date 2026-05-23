package com.orasaka.core.context;

import java.util.Collections;
import java.util.Map;
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
 * @param roles The resolved security roles tied to the user session.
 */
public record OrasakaContext(
    String userId, String conversationId, Map<String, Object> preferences, Set<String> roles) {
  public OrasakaContext {
    // Defensive copy of preferences
    preferences =
        (preferences != null)
            ? Collections.unmodifiableMap(Map.copyOf(preferences))
            : Collections.emptyMap();
    roles =
        (roles != null) ? Collections.unmodifiableSet(Set.copyOf(roles)) : Collections.emptySet();
  }

  /**
   * Checks if the user session has the specified role name.
   *
   * <p>This checking operation is thread-safe and non-blocking under Virtual Thread execution
   * contexts, as it queries the immutable local set of user roles. The input parameter is
   * normalized internally to uppercase and stripped of whitespace.
   *
   * @param roleName The name of the role to look up.
   * @return {@code true} if the role is present in the session context, otherwise {@code false}.
   */
  public boolean hasRole(String roleName) {
    if (roleName == null) {
      return false;
    }
    return roles.stream()
        .anyMatch(role -> role.toUpperCase().strip().equals(roleName.toUpperCase().strip()));
  }
}
