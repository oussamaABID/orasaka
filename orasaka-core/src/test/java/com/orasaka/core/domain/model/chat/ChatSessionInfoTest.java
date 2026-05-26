package com.orasaka.core.domain.model.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

class ChatSessionInfoTest {

  @Test
  @DisplayName("ChatSessionInfo constructor validates input fields")
  void validation() {
    Instant now = Instant.now();
    ChatSessionInfo valid = new ChatSessionInfo("session-1", USER_DASH_1, "title-1", now);
    assertNotNull(valid);

    assertThrows(
        NullPointerException.class, () -> new ChatSessionInfo(null, USER_DASH_1, "title-1", now));
    assertThrows(
        NullPointerException.class, () -> new ChatSessionInfo("session-1", null, "title-1", now));
    assertThrows(
        NullPointerException.class, () -> new ChatSessionInfo("session-1", USER_DASH_1, null, now));
    assertThrows(
        NullPointerException.class,
        () -> new ChatSessionInfo("session-1", USER_DASH_1, "title-1", null));

    assertEquals("session-1", valid.id());
    assertEquals(USER_DASH_1, valid.userId());
    assertEquals("title-1", valid.title());
    assertEquals(now, valid.updatedAt());
  }
}
