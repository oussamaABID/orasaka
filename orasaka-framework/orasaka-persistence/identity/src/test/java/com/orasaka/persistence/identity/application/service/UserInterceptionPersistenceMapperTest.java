package com.orasaka.persistence.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.identity.domain.model.UserInterceptionDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserInterceptionEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserInterceptionId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserInterceptionPersistenceMapperTest {

  @Test
  void toDto_mapsAllFields() {
    var id = new UserInterceptionId("user-1", "REFINER");
    var entity = new UserInterceptionEntity();
    entity.setId(id);
    Instant now = Instant.now();
    entity.setCreatedAt(now);
    UserInterceptionDto dto = UserInterceptionPersistenceMapper.toDto(entity);
    assertEquals("user-1", dto.userId());
    assertEquals("REFINER", dto.interceptionType());
    assertTrue(dto.active());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(UserInterceptionPersistenceMapper.toDto(null));
  }

  @Test
  void toEntity_mapsAllFields() {
    Instant now = Instant.now();
    var dto = new UserInterceptionDto("user-1", "MEMORY", true, now);
    var entity = UserInterceptionPersistenceMapper.toEntity(dto, "schema-1");
    assertEquals("user-1", entity.getId().userId());
    assertEquals("MEMORY", entity.getId().interceptionType());
    assertEquals("schema-1", entity.getSchemaId());
    assertEquals(now, entity.getCreatedAt());
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(UserInterceptionPersistenceMapper.toEntity(null, "schema"));
  }
}
