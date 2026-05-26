package com.orasaka.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class UserProfileTest {

  @Test
  void validConstruction_setsAllFields() {
    var profile =
        new UserProfile("user-1", "dark", "shimmer", "finance", "creative", Map.of("key", "val"));
    assertEquals("user-1", profile.userId());
    assertEquals("dark", profile.theme());
    assertEquals("shimmer", profile.voiceModel());
    assertEquals("finance", profile.primaryIndustry());
    assertEquals("creative", profile.aiBehavior());
    assertEquals(Map.of("key", "val"), profile.rawPreferences());
  }

  @Test
  void nullUserId_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new UserProfile(null, "dark", "alloy", "tech", "balanced", null));
  }

  @Test
  void nullTheme_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new UserProfile("user", null, "alloy", "tech", "balanced", null));
  }

  @Test
  void nullVoiceModel_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new UserProfile("user", "dark", null, "tech", "balanced", null));
  }

  @Test
  void nullPrimaryIndustry_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new UserProfile("user", "dark", "alloy", null, "balanced", null));
  }

  @Test
  void nullRawPreferences_defaultsToEmptyMap() {
    var profile = new UserProfile("user", "dark", "alloy", "tech", "balanced", null);
    assertNotNull(profile.rawPreferences());
    assertTrue(profile.rawPreferences().isEmpty());
  }

  @Test
  void rawPreferences_isDefensivelyCopied() {
    var mutable = new java.util.HashMap<String, Object>();
    mutable.put("key", "value");
    var profile = new UserProfile("user", "dark", "alloy", "tech", "balanced", mutable);
    var rawPrefs = profile.rawPreferences();
    assertThrows(UnsupportedOperationException.class, () -> rawPrefs.put("new", "val"));
  }
}
