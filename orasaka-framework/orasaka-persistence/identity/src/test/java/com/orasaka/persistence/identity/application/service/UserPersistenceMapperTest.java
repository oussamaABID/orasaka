package com.orasaka.persistence.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.identity.domain.model.UserDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AuthorityEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserPersistenceMapperTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void toDto_mapsAllFields() {
    var entity = new UserEntity();
    entity.setId("user-1");
    entity.setUsername("john");
    entity.setPasswordHash("hash");
    entity.setEmail("john@test.com");
    entity.setEnabled(true);
    entity.setPreferences(Map.of("lang", "en"));
    entity.setProvider("local");
    entity.setProviderId("pid");
    entity.setRateLimitTier("free");
    Instant now = Instant.now(FIXED_CLOCK);
    entity.setCreatedAt(now);

    var auth = new AuthorityEntity();
    auth.setAuthorityName("ROLE_USER");
    entity.setAuthorities(Set.of(auth));

    UserDto dto = UserPersistenceMapper.toDto(entity);
    assertEquals("user-1", dto.id());
    assertEquals("john", dto.username());
    assertEquals("hash", dto.passwordHash());
    assertEquals("john@test.com", dto.email());
    assertTrue(dto.enabled());
    assertEquals(Map.of("lang", "en"), dto.preferences());
    assertEquals(Set.of("ROLE_USER"), dto.authorities());
    assertEquals("local", dto.provider());
    assertEquals("pid", dto.providerId());
    assertEquals("free", dto.rateLimitTier());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(UserPersistenceMapper.toDto(null));
  }

  @Test
  void toDto_nullAuthorities_returnsEmptySet() {
    var entity = new UserEntity();
    entity.setId("user-2");
    entity.setUsername("jane");
    entity.setEmail("jane@test.com");
    entity.setEnabled(true);
    entity.setCreatedAt(Instant.now(FIXED_CLOCK));
    entity.setAuthorities(null);
    entity.setRateLimitTier("free");
    UserDto dto = UserPersistenceMapper.toDto(entity);
    assertTrue(dto.authorities().isEmpty());
  }

  @Test
  void toEntity_mapsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto =
        new UserDto(
            "user-1",
            "john",
            "hash",
            "john@test.com",
            true,
            Map.of("lang", "en"),
            Set.of("ROLE_USER"),
            List.of("REFINER"),
            "local",
            "pid",
            "free",
            now);
    var entity = UserPersistenceMapper.toEntity(dto);
    assertEquals("user-1", entity.getId());
    assertEquals("john", entity.getUsername());
    assertEquals("hash", entity.getPasswordHash());
    assertEquals("john@test.com", entity.getEmail());
    assertTrue(entity.getEnabled());
    assertEquals(Map.of("lang", "en"), entity.getPreferences());
    assertEquals("local", entity.getProvider());
    assertEquals("pid", entity.getProviderId());
    assertEquals("free", entity.getRateLimitTier());
    assertEquals(now, entity.getCreatedAt());
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(UserPersistenceMapper.toEntity(null));
  }
}
