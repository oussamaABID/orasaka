package com.orasaka.persistence.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.domain.model.ChatSessionDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.ChatSessionEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChatSessionMapperTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void toEntity_mapsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto = new ChatSessionDto("sess-1", "user-1", "My Chat", now);
    ChatSessionEntity entity = ChatSessionMapper.toEntity(dto);
    assertEquals("sess-1", entity.getId());
    assertEquals("user-1", entity.getUserId());
    assertEquals("My Chat", entity.getTitle());
    assertEquals(now, entity.getUpdatedAt());
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(ChatSessionMapper.toEntity(null));
  }

  @Test
  void toDto_mapsAllFields() {
    var entity = new ChatSessionEntity();
    entity.setId("sess-2");
    entity.setUserId("user-2");
    entity.setTitle("Chat Two");
    Instant now = Instant.now(FIXED_CLOCK);
    entity.setUpdatedAt(now);
    ChatSessionDto dto = ChatSessionMapper.toDto(entity);
    assertEquals("sess-2", dto.id());
    assertEquals("user-2", dto.userId());
    assertEquals("Chat Two", dto.title());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(ChatSessionMapper.toDto(null));
  }

  @Test
  void roundTrip_preservesData() {
    Instant now = Instant.now(FIXED_CLOCK);
    var original = new ChatSessionDto("id", "user", "title", now);
    ChatSessionDto roundTripped = ChatSessionMapper.toDto(ChatSessionMapper.toEntity(original));
    assertEquals(original, roundTripped);
  }
}
