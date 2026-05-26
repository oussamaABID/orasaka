package com.orasaka.workers.identity.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserRegisteredEvent;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentityNotificationListenerTest {

  @Test
  @DisplayName("Should process UserRegisteredEvent successfully")
  void shouldProcessUserRegisteredEvent() {
    var user =
        new User(
            UUID.randomUUID(), "john_doe", "john@example.com", true, Set.of("ROLE_USER"), null);
    var event = new UserRegisteredEvent(user, "test-token-123");
    var listener = new IdentityNotificationListener();

    // The listener only logs output, but we verify it can be invoked without exception.
    listener.onUserRegistered(event);

    assertThat(event.user().email()).isEqualTo("john@example.com");
    assertThat(event.plaintextToken()).isEqualTo("test-token-123");
  }
}
