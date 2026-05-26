package com.orasaka.workers.identity.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.orasaka.identity.domain.model.PasswordResetRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordNotificationListenerTest {

  @Test
  @DisplayName("Should process PasswordResetRequestedEvent successfully")
  void shouldProcessPasswordResetRequestedEvent() {
    var event = new PasswordResetRequestedEvent("john@example.com", "reset-token-456");
    var listener = new PasswordNotificationListener();

    // The listener only logs output, but we verify it can be invoked without exception.
    listener.onPasswordResetRequested(event);

    assertThat(event.email()).isEqualTo("john@example.com");
    assertThat(event.plaintextToken()).isEqualTo("reset-token-456");
  }
}
