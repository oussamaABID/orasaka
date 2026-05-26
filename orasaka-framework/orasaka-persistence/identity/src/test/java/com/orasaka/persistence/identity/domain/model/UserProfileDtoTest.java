package com.orasaka.persistence.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class UserProfileDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    var dto =
        new UserProfileDto(
            "user-1", "dark", "shimmer", "finance", "creative", Map.of("key", "value"));
    assertEquals("user-1", dto.userId());
    assertEquals("dark", dto.theme());
    assertEquals("shimmer", dto.voiceModel());
    assertEquals("finance", dto.primaryIndustry());
    assertEquals("creative", dto.aiBehavior());
    assertEquals(Map.of("key", "value"), dto.rawPreferences());
  }

  @Test
  void nullUserId_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new UserProfileDto(null, "dark", "alloy", "tech", "balanced", null));
  }

  @Test
  void nullTheme_defaultsToEmerald() {
    var dto = new UserProfileDto("user-1", null, "alloy", "tech", "balanced", null);
    assertEquals("emerald", dto.theme());
  }

  @Test
  void nullVoiceModel_defaultsToAlloy() {
    var dto = new UserProfileDto("user-1", "dark", null, "tech", "balanced", null);
    assertEquals("alloy", dto.voiceModel());
  }

  @Test
  void nullPrimaryIndustry_defaultsToTech() {
    var dto = new UserProfileDto("user-1", "dark", "alloy", null, "balanced", null);
    assertEquals("tech", dto.primaryIndustry());
  }

  @Test
  void nullRawPreferences_allowed() {
    var dto = new UserProfileDto("user-1", "dark", "alloy", "tech", "balanced", null);
    assertNull(dto.rawPreferences());
  }

  @Test
  void allDefaults_appliedCorrectly() {
    var dto = new UserProfileDto("user-1", null, null, null, null, null);
    assertEquals("emerald", dto.theme());
    assertEquals("alloy", dto.voiceModel());
    assertEquals("tech", dto.primaryIndustry());
  }
}
