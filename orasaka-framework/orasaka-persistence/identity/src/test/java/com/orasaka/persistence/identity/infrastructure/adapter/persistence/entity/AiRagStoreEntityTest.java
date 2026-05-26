package com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AiRagStoreEntity} getter/setter coverage. */
class AiRagStoreEntityTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void defaultConstructor_setsDefaults() {
    AiRagStoreEntity entity = new AiRagStoreEntity();
    assertNull(entity.getId());
    assertNull(entity.getUserId());
    assertNull(entity.getName());
  }

  @Test
  void settersAndGetters_roundTrip() {
    AiRagStoreEntity entity = new AiRagStoreEntity();
    Instant now = Instant.now(FIXED_CLOCK);

    entity.setId(1);
    entity.setUserId("user-abc");
    entity.setName("my-rag-store");
    entity.setStoreType("pgvector");
    entity.setHost("db.orasaka.io");
    entity.setPort(5432);
    entity.setDatabaseName("orasaka_rag");
    entity.setTableName("embeddings");
    entity.setUsername("admin");
    entity.setPassword("secret");
    entity.setEnabled(true);
    entity.setCreatedAt(now);

    assertEquals(1, entity.getId());
    assertEquals("user-abc", entity.getUserId());
    assertEquals("my-rag-store", entity.getName());
    assertEquals("pgvector", entity.getStoreType());
    assertEquals("db.orasaka.io", entity.getHost());
    assertEquals(5432, entity.getPort());
    assertEquals("orasaka_rag", entity.getDatabaseName());
    assertEquals("embeddings", entity.getTableName());
    assertEquals("admin", entity.getUsername());
    assertEquals("secret", entity.getPassword());
    assertTrue(entity.getEnabled());
    assertEquals(now, entity.getCreatedAt());
  }
}
