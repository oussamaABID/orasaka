package com.orasaka.persistence.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AiMcpServerDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    Instant now = Instant.now();
    var dto = new AiMcpServerDto(1, "user-1", "mcp-1", "http://localhost:8080", true, now);
    assertEquals(1, dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("mcp-1", dto.name());
    assertEquals("http://localhost:8080", dto.url());
    assertTrue(dto.enabled());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void nullUserId_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new AiMcpServerDto(1, null, "name", "url", true, now));
  }

  @Test
  void nullName_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new AiMcpServerDto(1, "user", null, "url", true, now));
  }

  @Test
  void nullUrl_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new AiMcpServerDto(1, "user", "name", null, true, now));
  }

  @Test
  void nullEnabled_defaultsToTrue() {
    var dto = new AiMcpServerDto(1, "user", "name", "url", null, Instant.now());
    assertTrue(dto.enabled());
  }
}
