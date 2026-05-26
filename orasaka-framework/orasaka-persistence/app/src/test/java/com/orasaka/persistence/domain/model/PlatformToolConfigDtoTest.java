package com.orasaka.persistence.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformToolConfigDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    var dto =
        new PlatformToolConfigDto(
            1, "tool-1", true, 600, false, "FIXED", "custom_table", Instant.now());
    assertEquals(1, dto.id());
    assertEquals("tool-1", dto.toolId());
    assertTrue(dto.cacheEnabled());
    assertEquals(600, dto.cacheTtlSeconds());
    assertFalse(dto.ragEnabled());
    assertEquals("FIXED", dto.chunkerType());
    assertEquals("custom_table", dto.sourceTable());
  }

  @Test
  void nullToolId_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class,
        () -> new PlatformToolConfigDto(1, null, true, 600, true, "MD", "table", now));
  }

  @Test
  void nullCacheEnabled_defaultsToTrue() {
    var dto = new PlatformToolConfigDto(1, "tool-1", null, 600, true, "MD", "table", Instant.now());
    assertTrue(dto.cacheEnabled());
  }

  @Test
  void nullCacheTtl_defaultsTo3600() {
    var dto =
        new PlatformToolConfigDto(1, "tool-1", true, null, true, "MD", "table", Instant.now());
    assertEquals(3600, dto.cacheTtlSeconds());
  }

  @Test
  void nullRagEnabled_defaultsToTrue() {
    var dto = new PlatformToolConfigDto(1, "tool-1", true, 600, null, "MD", "table", Instant.now());
    assertTrue(dto.ragEnabled());
  }

  @Test
  void nullChunkerType_defaultsToMarkdown() {
    var dto = new PlatformToolConfigDto(1, "tool-1", true, 600, true, null, "table", Instant.now());
    assertEquals("MARKDOWN_CHUNKERS", dto.chunkerType());
  }

  @Test
  void nullSourceTable_defaultsToRagSource() {
    var dto = new PlatformToolConfigDto(1, "tool-1", true, 600, true, "MD", null, Instant.now());
    assertEquals("orasaka_tools_rag_source", dto.sourceTable());
  }

  @Test
  void allNullDefaults_appliedCorrectly() {
    var dto = new PlatformToolConfigDto(null, "tool-1", null, null, null, null, null, null);
    assertTrue(dto.cacheEnabled());
    assertEquals(3600, dto.cacheTtlSeconds());
    assertTrue(dto.ragEnabled());
    assertEquals("MARKDOWN_CHUNKERS", dto.chunkerType());
    assertEquals("orasaka_tools_rag_source", dto.sourceTable());
  }
}
