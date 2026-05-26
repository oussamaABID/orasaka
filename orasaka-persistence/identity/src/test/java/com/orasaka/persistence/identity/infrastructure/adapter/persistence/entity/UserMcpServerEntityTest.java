package com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link UserMcpServerEntity} getter/setter coverage. */
class UserMcpServerEntityTest {

  @Test
  void defaultConstructor_setsDefaults() {
    UserMcpServerEntity entity = new UserMcpServerEntity();
    assertNull(entity.getId());
    assertTrue(entity.getEnabled());
    assertNotNull(entity.getCreatedAt());
  }

  @Test
  void settersAndGetters_roundTrip() {
    UserMcpServerEntity entity = new UserMcpServerEntity();
    Instant now = Instant.now();

    entity.setId(42);
    entity.setUserId("user-xyz");
    entity.setLabel("My MCP Server");
    entity.setUrl("https://mcp.orasaka.io");
    entity.setAuthToken("bearer-token-123");
    entity.setEnabled(false);
    entity.setCreatedAt(now);

    assertEquals(42, entity.getId());
    assertEquals("user-xyz", entity.getUserId());
    assertEquals("My MCP Server", entity.getLabel());
    assertEquals("https://mcp.orasaka.io", entity.getUrl());
    assertEquals("bearer-token-123", entity.getAuthToken());
    assertFalse(entity.getEnabled());
    assertEquals(now, entity.getCreatedAt());
  }
}
