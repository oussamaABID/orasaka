package com.orasaka.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Role} sealed interface hierarchy. */
class RoleTest {

  @Test
  @DisplayName("Admin role returns ADMIN")
  void adminName() {
    assertEquals("ADMIN", new Role.Admin().name());
  }

  @Test
  @DisplayName("User role returns USER")
  void userName() {
    assertEquals("USER", new Role.User().name());
  }

  @Test
  @DisplayName("Guest role returns GUEST")
  void guestName() {
    assertEquals("GUEST", new Role.Guest().name());
  }

  @Test
  @DisplayName("Role sealed interface is exhaustive with 3 variants")
  void exhaustiveSealedInterface() {
    Role role = new Role.Admin();
    String name =
        switch (role) {
          case Role.Admin a -> a.name();
          case Role.User u -> u.name();
          case Role.Guest g -> g.name();
        };
    assertEquals("ADMIN", name);
  }
}
