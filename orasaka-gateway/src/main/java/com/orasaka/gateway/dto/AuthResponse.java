package com.orasaka.gateway.dto;

import java.util.List;
import java.util.Objects;

/**
 * Strongly-typed authentication response DTO for the REST ingress layer.
 *
 * <p>Replaces raw {@code Map.of()} responses with a fail-fast, self-validating record (ERR-106).
 * Domain-blind — contains only primitive gateway-scoped fields.
 *
 * @param token The user's authentication token (UUID string).
 * @param username The authenticated user's display name.
 * @param activeInterceptions List of currently active interception types for the user.
 */
public record AuthResponse(String token, String username, List<String> activeInterceptions) {

  /** Compact constructor enforcing fail-fast invariants. */
  public AuthResponse {
    Objects.requireNonNull(token, "Auth token is required");
    Objects.requireNonNull(username, "Username is required");
    activeInterceptions =
        activeInterceptions != null ? List.copyOf(activeInterceptions) : List.of();
  }
}
