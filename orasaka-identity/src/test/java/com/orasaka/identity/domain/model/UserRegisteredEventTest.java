package com.orasaka.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link UserRegisteredEvent} record. */
class UserRegisteredEventTest {

  @Test
  @DisplayName("preserves user and token")
  void preservesFields() {
    var user = new User(UUID.randomUUID(), "admin", "a@b.com", true, null, null);
    var event = new UserRegisteredEvent(user, "tok-123");
    assertSame(user, event.user());
    assertEquals("tok-123", event.plaintextToken());
  }

  @Test
  @DisplayName("null token accepted")
  void nullTokenAccepted() {
    var user = new User(UUID.randomUUID(), "admin", "a@b.com", true, null, null);
    var event = new UserRegisteredEvent(user, null);
    assertNull(event.plaintextToken());
  }
}
