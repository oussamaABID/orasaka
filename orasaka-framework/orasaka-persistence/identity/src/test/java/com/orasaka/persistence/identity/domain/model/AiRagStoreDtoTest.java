package com.orasaka.persistence.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AiRagStoreDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    Instant now = Instant.now();
    var dto =
        new AiRagStoreDto(
            1,
            "user-1",
            "My RAG",
            "pgvector",
            "localhost",
            5432,
            "rag_db",
            "vectors",
            "admin",
            "secret",
            true,
            now);
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
  void nullUserId_throws() {
    assertThrows(
        NullPointerException.class,
        () ->
            new AiRagStoreDto(
                1, null, "name", "type", null, null, null, null, null, null, null, null));
  }

  @Test
  void nullName_throws() {
    assertThrows(
        NullPointerException.class,
        () ->
            new AiRagStoreDto(
                1, "user", null, "type", null, null, null, null, null, null, null, null));
  }

  @Test
  void nullStoreType_throws() {
    assertThrows(
        NullPointerException.class,
        () ->
            new AiRagStoreDto(
                1, "user", "name", null, null, null, null, null, null, null, null, null));
  }

  @Test
  void nullEnabled_defaultsToTrue() {
    var dto =
        new AiRagStoreDto(
            1, "user", "name", "type", null, null, null, null, null, null, null, null);
    assertTrue(dto.enabled());
  }
}
