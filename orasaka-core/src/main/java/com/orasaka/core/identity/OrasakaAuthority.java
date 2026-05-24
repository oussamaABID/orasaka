package com.orasaka.core.identity;

/**
 * Immutable security role representation for the Orasaka ecosystem.
 *
 * @param name The name of the authority/role (e.g., "ROLE_USER").
 */
public record OrasakaAuthority(String name) {
  public OrasakaAuthority {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Authority name cannot be null or empty");
    }
  }
}
