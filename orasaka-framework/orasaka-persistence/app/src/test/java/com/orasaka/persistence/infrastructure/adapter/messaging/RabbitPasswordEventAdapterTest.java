package com.orasaka.persistence.infrastructure.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.orasaka.identity.domain.model.PasswordResetRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitPasswordEventAdapterTest {

  @Mock private RabbitTemplate rabbitTemplate;

  private RabbitPasswordEventAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new RabbitPasswordEventAdapter(rabbitTemplate);
  }

  @Test
  void shouldPublishEvent() {
    PasswordResetRequestedEvent event =
        new PasswordResetRequestedEvent("test@orasaka.com", "token123");

    adapter.publish(event);

    verify(rabbitTemplate).convertAndSend("orasaka.identity.events", "password.reset", event);
  }

  @Test
  void shouldThrowWhenEventIsNull() {
    assertThrows(NullPointerException.class, () -> adapter.publish(null));
  }
}
