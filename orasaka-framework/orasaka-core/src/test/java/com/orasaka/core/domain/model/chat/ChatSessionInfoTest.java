package com.orasaka.core.domain.model.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChatSessionInfoTest {

  @Test
  void validConstruction() {
    Instant now = Instant.now();
    var info = new ChatSessionInfo("id", "user", "Title", now);
    assertEquals("id", info.id());
    assertEquals("user", info.userId());
    assertEquals("Title", info.title());
    assertEquals(now, info.updatedAt());
  }

  @Test
  void nullId_throws() {
    var now = Instant.now();
    assertThrows(NullPointerException.class, () -> new ChatSessionInfo(null, "user", "title", now));
  }

  @Test
  void nullUserId_throws() {
    var now = Instant.now();
    assertThrows(NullPointerException.class, () -> new ChatSessionInfo("id", null, "title", now));
  }

  @Test
  void nullTitle_throws() {
    var now = Instant.now();
    assertThrows(NullPointerException.class, () -> new ChatSessionInfo("id", "user", null, now));
  }

  @Test
  void nullUpdatedAt_throws() {
    assertThrows(
        NullPointerException.class, () -> new ChatSessionInfo("id", "user", "title", null));
  }
}
