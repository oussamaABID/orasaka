package com.orasaka.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link User} domain record — compact constructor, defaults, and invariants. */
class UserTest {

  private static final UUID ID = UUID.randomUUID();

  @Nested
  @DisplayName("Compact constructor invariants")
  class Invariants {

    @Test
    @DisplayName("null ID throws NPE")
    void nullId() {
      assertThrows(
          NullPointerException.class,
          () -> new User(null, "user", "u@e.com", true, null, null, null, null));
    }

    @Test
    @DisplayName("null username throws IAE")
    void nullUsername() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new User(ID, null, "u@e.com", true, null, null, null, null));
    }

    @Test
    @DisplayName("blank username throws IAE")
    void blankUsername() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new User(ID, "  ", "u@e.com", true, null, null, null, null));
    }

    @Test
    @DisplayName("null email throws IAE")
    void nullEmail() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new User(ID, "user", null, true, null, null, null, null));
    }

    @Test
    @DisplayName("blank email throws IAE")
    void blankEmail() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new User(ID, "user", "  ", true, null, null, null, null));
    }
  }

  @Nested
  @DisplayName("Defensive defaults")
  class Defaults {

    @Test
    @DisplayName("null authorities defaults to empty set")
    void nullAuthorities() {
      var user = new User(ID, "user", "u@e.com", true, null, null, null, null);
      assertNotNull(user.authorities());
      assertTrue(user.authorities().isEmpty());
    }

    @Test
    @DisplayName("null preferences defaults with language=en")
    void nullPreferences() {
      var user = new User(ID, "user", "u@e.com", true, null, null, null, null);
      assertNotNull(user.preferences());
      assertEquals("en", user.preferences().get("language"));
    }

    @Test
    @DisplayName("existing language is preserved")
    void existingLanguage() {
      var user = new User(ID, "user", "u@e.com", true, null, Map.of("language", "fr"), null, null);
      assertEquals("fr", user.preferences().get("language"));
    }

    @Test
    @DisplayName("null activeInterceptions defaults to empty list")
    void nullInterceptions() {
      var user = new User(ID, "user", "u@e.com", true, null, null, null, null);
      assertNotNull(user.activeInterceptions());
      assertTrue(user.activeInterceptions().isEmpty());
    }

    @Test
    @DisplayName("authorities are immutable copies")
    void authoritiesImmutable() {
      var user = new User(ID, "user", "u@e.com", true, Set.of("ADMIN"), null, null, null);
      var auths = user.authorities();
      assertThrows(UnsupportedOperationException.class, () -> auths.add("NEW"));
    }
  }

  @Nested
  @DisplayName("Overloaded constructors")
  class OverloadedConstructors {

    @Test
    @DisplayName("7-arg constructor auto-generates UUID if null")
    void sevenArgNullId() {
      var user = new User(null, "user", "u@e.com", true, null, null, null);
      assertNotNull(user.id());
    }

    @Test
    @DisplayName("6-arg constructor works")
    void sixArgConstructor() {
      var user = new User(ID, "user", "u@e.com", true, Set.of("USER"), Map.of());
      assertNotNull(user);
      assertTrue(user.activeInterceptions().isEmpty());
    }
  }
}
