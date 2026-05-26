package com.orasaka.persistence.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.domain.model.PlatformMcpServerDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PlatformMcpServerEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformMcpServerMapperTest {

  @Test
  void toDto_mapsAllFields() {
    var entity = new PlatformMcpServerEntity();
    entity.setId(1);
    entity.setLabel("GitHub MCP");
    entity.setTransportType("STDIO");
    entity.setUrl("http://localhost");
    entity.setCommand("node");
    entity.setArgs("--port 3000");
    entity.setAuthToken("token");
    entity.setEnabled(true);
    Instant now = Instant.now();
    entity.setCreatedAt(now);
    PlatformMcpServerDto dto = PlatformMcpServerMapper.toDto(entity);
    assertEquals(1, dto.id());
    assertEquals("GitHub MCP", dto.label());
    assertEquals("STDIO", dto.transportType());
    assertEquals("http://localhost", dto.url());
    assertEquals("node", dto.command());
    assertEquals("--port 3000", dto.args());
    assertEquals("token", dto.authToken());
    assertTrue(dto.enabled());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(PlatformMcpServerMapper.toDto(null));
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(PlatformMcpServerMapper.toEntity(null));
  }

  @Test
  void toEntity_mapsAllFields() {
    Instant now = Instant.now();
    var dto = new PlatformMcpServerDto(1, "MCP", "SSE", "url", "cmd", "args", "token", true, now);
    var entity = PlatformMcpServerMapper.toEntity(dto);
    assertEquals(1, entity.getId());
    assertEquals("MCP", entity.getLabel());
    assertEquals("SSE", entity.getTransportType());
    assertEquals("url", entity.getUrl());
    assertEquals("cmd", entity.getCommand());
    assertEquals("args", entity.getArgs());
    assertEquals("token", entity.getAuthToken());
    assertTrue(entity.getEnabled());
    assertEquals(now, entity.getCreatedAt());
  }
}
