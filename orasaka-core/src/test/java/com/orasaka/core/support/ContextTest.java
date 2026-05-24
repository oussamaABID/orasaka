package com.orasaka.core.support;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Context} record — preferences, authorities, and immutability. */
class ContextTest {

  @Nested
  @DisplayName("Defaults")
  class Defaults {

    @Test
    @DisplayName("null preferences defaults to empty map")
    void nullPrefsDefault() {
      var ctx = new Context("user1", "conv1", null, null);
      assertNotNull(ctx.preferences());
      assertTrue(ctx.preferences().isEmpty());
    }

    @Test
    @DisplayName("null authorities defaults to empty set")
    void nullAuthoritiesDefault() {
      var ctx = new Context("user1", "conv1", null, null);
      assertNotNull(ctx.authorities());
      assertTrue(ctx.authorities().isEmpty());
    }
  }

  @Nested
  @DisplayName("Authority checks")
  class AuthorityChecks {

    @Test
    @DisplayName("hasAuthority matches case-insensitively")
    void hasAuthority() {
      var ctx = new Context("user1", "conv1", Map.of(), Set.of(new Authority("ADMIN")));
      assertTrue(ctx.hasAuthority("admin"));
      assertTrue(ctx.hasAuthority("ADMIN"));
    }

    @Test
    @DisplayName("hasAuthority returns false when not present")
    void hasAuthorityFalse() {
      var ctx = new Context("user1", "conv1", Map.of(), Set.of());
      assertFalse(ctx.hasAuthority("ADMIN"));
    }
  }

  @Nested
  @DisplayName("Immutability")
  class Immutability {

    @Test
    @DisplayName("preferences map is immutable")
    void preferencesImmutable() {
      var ctx = new Context("u", "c", Map.of("k", "v"), Set.of());
      assertThrows(UnsupportedOperationException.class, () -> ctx.preferences().put("x", "y"));
    }
  }
}
