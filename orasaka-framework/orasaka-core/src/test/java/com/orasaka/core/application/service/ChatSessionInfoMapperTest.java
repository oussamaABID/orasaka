package com.orasaka.core.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.chat.ChatSessionInfo;
import com.orasaka.persistence.domain.model.ChatSessionDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChatSessionInfoMapperTest {

  @Test
  void toInfo_null_returnsNull() {
    assertNull(ChatSessionInfoMapper.toInfo(null));
  }

  @Test
  void toInfo_validDto_mapsAllFields() {
    Instant now = Instant.now();
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
    Instant now = Instant.now();
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
    Instant now = Instant.now();
    var original = new ChatSessionDto("sess-1", "user-1", "Chat Title", now);

    ChatSessionInfo info = ChatSessionInfoMapper.toInfo(original);
    ChatSessionDto roundTripped = ChatSessionInfoMapper.toDto(info);

    assertEquals(original, roundTripped);
  }
}
