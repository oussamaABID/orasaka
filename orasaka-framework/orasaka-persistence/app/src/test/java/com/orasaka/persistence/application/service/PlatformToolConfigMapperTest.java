package com.orasaka.persistence.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.domain.model.PlatformToolConfigDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PlatformToolConfigEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformToolConfigMapperTest {

  @Test
  void toDto_mapsAllFields() {
    var entity = new PlatformToolConfigEntity();
    entity.setId(1);
    entity.setToolId("tool-1");
    entity.setCacheEnabled(true);
    entity.setCacheTtlSeconds(600);
    entity.setRagEnabled(false);
    entity.setChunkerType("FIXED");
    entity.setSourceTable("custom_table");
    Instant now = Instant.now();
    entity.setCreatedAt(now);
    PlatformToolConfigDto dto = PlatformToolConfigMapper.toDto(entity);
    assertEquals(1, dto.id());
    assertEquals("tool-1", dto.toolId());
    assertTrue(dto.cacheEnabled());
    assertEquals(600, dto.cacheTtlSeconds());
    assertFalse(dto.ragEnabled());
    assertEquals("FIXED", dto.chunkerType());
    assertEquals("custom_table", dto.sourceTable());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(PlatformToolConfigMapper.toDto(null));
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(PlatformToolConfigMapper.toEntity(null));
  }

  @Test
  void toEntity_mapsAllFields() {
    Instant now = Instant.now();
    var dto = new PlatformToolConfigDto(1, "tool-1", true, 600, false, "FIXED", "table", now);
    var entity = PlatformToolConfigMapper.toEntity(dto);
    assertEquals(1, entity.getId());
    assertEquals("tool-1", entity.getToolId());
    assertTrue(entity.getCacheEnabled());
    assertEquals(600, entity.getCacheTtlSeconds());
    assertFalse(entity.getRagEnabled());
    assertEquals("FIXED", entity.getChunkerType());
    assertEquals("table", entity.getSourceTable());
    assertEquals(now, entity.getCreatedAt());
  }
}
