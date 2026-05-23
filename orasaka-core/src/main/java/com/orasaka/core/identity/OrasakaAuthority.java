package com.orasaka.core.identity;

/**
 * Represents a dynamic runtime authority or role assigned to a user session.
 *
 * <p>Roles and authorities are strictly decoupled from hardcoded enums, allowing flexible
 * multi-tenant business rules mapping. This structure is thread-safe and fully compliant with
 * Virtual Thread execution environments.
 *
 * @param name The normalized uppercase string representation of the authority.
 */
public record OrasakaAuthority(String name) {
  public OrasakaAuthority {
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("Authority name cannot be blank");
    name = name.toUpperCase().strip();
  }
}
