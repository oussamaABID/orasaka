package com.orasaka.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.orasaka.core.domain.model.chat.ChatSessionInfo;
import com.orasaka.persistence.domain.model.ChatSessionDto;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

class ChatSessionInfoMapperTest {

  @Test
  @DisplayName("map to info handles null and maps correct values")
  void mapToInfo() {
    assertNull(ChatSessionInfoMapper.toInfo(null));

    Instant now = Instant.now();
    ChatSessionDto dto = new ChatSessionDto("session-1", USER_DASH_1, "Title 1", now);
    ChatSessionInfo info = ChatSessionInfoMapper.toInfo(dto);

    assertEquals("session-1", info.id());
    assertEquals(USER_DASH_1, info.userId());
    assertEquals("Title 1", info.title());
    assertEquals(now, info.updatedAt());
  }

  @Test
  @DisplayName("map to dto handles null and maps correct values")
  void mapToDto() {
    assertNull(ChatSessionInfoMapper.toDto(null));

    Instant now = Instant.now();
    ChatSessionInfo info = new ChatSessionInfo("session-1", USER_DASH_1, "Title 1", now);
    ChatSessionDto dto = ChatSessionInfoMapper.toDto(info);

    assertEquals("session-1", dto.id());
    assertEquals(USER_DASH_1, dto.userId());
    assertEquals("Title 1", dto.title());
    assertEquals(now, dto.updatedAt());
  }
}
