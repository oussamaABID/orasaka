package com.orasaka.core.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.chat.ChatSessionInfo;
import com.orasaka.persistence.domain.model.ChatSessionDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChatSessionInfoMapperTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void toInfo_null_returnsNull() {
    assertNull(ChatSessionInfoMapper.toInfo(null));
  }

  @Test
  void toInfo_validDto_mapsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto = new ChatSessionDto("sess-1", "user-1", "My Chat", now);

    ChatSessionInfo result = ChatSessionInfoMapper.toInfo(dto);

    assertNotNull(result);
    assertEquals("sess-1", result.id());
    assertEquals("user-1", result.userId());
    assertEquals("My Chat", result.title());
    assertEquals(now, result.updatedAt());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(ChatSessionInfoMapper.toDto(null));
  }

  @Test
  void toDto_validInfo_mapsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var info = new ChatSessionInfo("sess-1", "user-1", "My Chat", now);

    ChatSessionDto result = ChatSessionInfoMapper.toDto(info);

    assertNotNull(result);
    assertEquals("sess-1", result.id());
    assertEquals("user-1", result.userId());
    assertEquals("My Chat", result.title());
    assertEquals(now, result.updatedAt());
  }

  @Test
  void roundTrip_preservesData() {
    Instant now = Instant.now(FIXED_CLOCK);
    var original = new ChatSessionDto("sess-1", "user-1", "Chat Title", now);

    ChatSessionInfo info = ChatSessionInfoMapper.toInfo(original);
    ChatSessionDto roundTripped = ChatSessionInfoMapper.toDto(info);

    assertEquals(original, roundTripped);
  }
}
