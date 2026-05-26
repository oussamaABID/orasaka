package com.orasaka.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PersonnaTest {

  @Test
  void freeUser_returnsValidUser() {
    User user = Personna.freeUser();
    assertNotNull(user);
    assertEquals("test-user", user.username());
    assertEquals("test-user@orasaka.com", user.email());
    assertTrue(user.enabled());
    assertEquals("free", user.rateLimitTier());
    assertTrue(user.authorities().contains("ROLE_USER"));
  }

  @Test
  void freeUser_withPreferences_mergesPreferences() {
    var prefs = Map.<String, Object>of("theme", "dark");
    User user = Personna.freeUser(prefs);
    assertEquals("dark", user.preferences().get("theme"));
  }

  @Test
  void premiumUser_returnsValidUser() {
    User user = Personna.premiumUser();
    assertNotNull(user);
    assertEquals("premium", user.rateLimitTier());
  }

  @Test
  void adminUser_returnsValidAdmin() {
    User user = Personna.adminUser();
    assertNotNull(user);
    assertEquals("admin", user.username());
    assertEquals("unlimited", user.rateLimitTier());
    assertTrue(user.authorities().contains("ROLE_ADMIN"));
    assertTrue(user.authorities().contains("ROLE_USER"));
  }
}
