package com.orasaka.identity.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.domain.model.UserProfile;
import com.orasaka.persistence.identity.domain.model.UserProfileDto;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserProfileMapperTest {

  @Test
  void toDomain_mapsAllFields() {
    var dto =
        new UserProfileDto(
            "user-1", "dark", "shimmer", "finance", "creative", Map.of("key", "value"));
    UserProfile domain = UserProfileMapper.toDomain(dto);
    assertEquals("user-1", domain.userId());
    assertEquals("dark", domain.theme());
    assertEquals("shimmer", domain.voiceModel());
    assertEquals("finance", domain.primaryIndustry());
    assertEquals("creative", domain.aiBehavior());
    assertEquals(Map.of("key", "value"), domain.rawPreferences());
  }

  @Test
  void toDomain_null_returnsNull() {
    assertNull(UserProfileMapper.toDomain(null));
  }

  @Test
  void toDomain_nullFields_applyDefaults() {
    var dto = new UserProfileDto("user-1", null, null, null, null, null);
    UserProfile domain = UserProfileMapper.toDomain(dto);
    assertEquals("emerald", domain.theme());
    assertEquals("alloy", domain.voiceModel());
    assertEquals("tech", domain.primaryIndustry());
    assertEquals(Map.of(), domain.rawPreferences());
  }

  @Test
  void toDto_mapsAllFields() {
    var domain =
        new UserProfile("user-1", "dark", "shimmer", "finance", "creative", Map.of("key", "value"));
    UserProfileDto dto = UserProfileMapper.toDto(domain);
    assertEquals("user-1", dto.userId());
    assertEquals("dark", dto.theme());
    assertEquals("shimmer", dto.voiceModel());
    assertEquals("finance", dto.primaryIndustry());
    assertEquals("creative", dto.aiBehavior());
    assertEquals(Map.of("key", "value"), dto.rawPreferences());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(UserProfileMapper.toDto(null));
  }
}
