package com.orasaka.gateway.dto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * GraphQL-compatible DTO record mapping exactly to the {@code type User} in the schema.
 *
 * <p>This record is the gateway's projection of a user identity — domain-blind and decoupled from
 * {@code com.orasaka.identity.domain.User}. It is used inside {@link RegisterResponse} for the
 * GraphQL {@code RegisterResult.user} field.
 *
 * @param id The user's unique identifier (UUID string).
 * @param username The user's display name.
 * @param email The user's email address.
 * @param authorities The user's granted authority names.
 * @param preferences The user's preference map.
 */
public record UserDescriptor(
    String id,
    String username,
    String email,
    List<String> authorities,
    Map<String, Object> preferences) {

  /** Compact constructor enforcing fail-fast invariants. */
  public UserDescriptor {
    Objects.requireNonNull(id, "User ID is required");
    Objects.requireNonNull(username, "Username is required");
    authorities = authorities != null ? List.copyOf(authorities) : List.of();
  }
}
