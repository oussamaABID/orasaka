package com.orasaka.persistence.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.identity.domain.model.AiRagStoreDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiRagStoreEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AiRagStorePersistenceMapperTest {

  @Test
  void toDto_mapsAllFields() {
    var entity = new AiRagStoreEntity();
    entity.setId(1);
    entity.setUserId("user-1");
    entity.setName("My RAG");
    entity.setStoreType("pgvector");
    entity.setHost("localhost");
    entity.setPort(5432);
    entity.setDatabaseName("rag_db");
    entity.setTableName("vectors");
    entity.setUsername("admin");
    entity.setPassword("secret");
    entity.setEnabled(true);
    Instant now = Instant.now();
    entity.setCreatedAt(now);
    AiRagStoreDto dto = AiRagStorePersistenceMapper.toDto(entity);
    assertEquals(1, dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("My RAG", dto.name());
    assertEquals("pgvector", dto.storeType());
    assertEquals("localhost", dto.host());
    assertEquals(5432, dto.port());
    assertEquals("rag_db", dto.databaseName());
    assertEquals("vectors", dto.tableName());
    assertEquals("admin", dto.username());
    assertEquals("secret", dto.password());
    assertTrue(dto.enabled());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(AiRagStorePersistenceMapper.toDto(null));
  }

  @Test
  void toEntity_mapsAllFields() {
    Instant now = Instant.now();
    var dto =
        new AiRagStoreDto(
            1, "user-1", "RAG", "pgvector", "host", 5432, "db", "table", "user", "pass", true, now);
    var entity = AiRagStorePersistenceMapper.toEntity(dto);
    assertEquals(1, entity.getId());
    assertEquals("user-1", entity.getUserId());
    assertEquals("RAG", entity.getName());
    assertEquals("pgvector", entity.getStoreType());
    assertEquals("host", entity.getHost());
    assertEquals(5432, entity.getPort());
    assertEquals("db", entity.getDatabaseName());
    assertEquals("table", entity.getTableName());
    assertEquals("user", entity.getUsername());
    assertEquals("pass", entity.getPassword());
    assertTrue(entity.getEnabled());
    assertEquals(now, entity.getCreatedAt());
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(AiRagStorePersistenceMapper.toEntity(null));
  }
}
