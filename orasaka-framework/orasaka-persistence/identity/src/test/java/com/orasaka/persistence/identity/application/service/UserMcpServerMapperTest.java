package com.orasaka.persistence.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.identity.domain.model.UserMcpServerDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserMcpServerEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserMcpServerMapperTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void toDto_mapsAllFields() {
    var entity = new UserMcpServerEntity();
    entity.setId(1);
    entity.setUserId("user-1");
    entity.setLabel("My MCP");
    entity.setUrl("http://localhost:3000");
    entity.setAuthToken("token");
    entity.setEnabled(true);
    Instant now = Instant.now(FIXED_CLOCK);
    entity.setCreatedAt(now);
    UserMcpServerDto dto = UserMcpServerMapper.toDto(entity);
    assertEquals(1, dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("My MCP", dto.label());
    assertEquals("http://localhost:3000", dto.url());
    assertEquals("token", dto.authToken());
    assertTrue(dto.enabled());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(UserMcpServerMapper.toDto(null));
  }

  @Test
  void toEntity_mapsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto = new UserMcpServerDto(1, "user-1", "MCP", "url", "token", true, now);
    var entity = UserMcpServerMapper.toEntity(dto);
    assertEquals(1, entity.getId());
    assertEquals("user-1", entity.getUserId());
    assertEquals("MCP", entity.getLabel());
    assertEquals("url", entity.getUrl());
    assertEquals("token", entity.getAuthToken());
    assertTrue(entity.getEnabled());
    assertEquals(now, entity.getCreatedAt());
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(UserMcpServerMapper.toEntity(null));
  }
}
