package com.orasaka.persistence.infrastructure.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserRegisteredEvent;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitUserEventAdapterTest {

  @Mock private RabbitTemplate rabbitTemplate;

  private RabbitUserEventAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new RabbitUserEventAdapter(rabbitTemplate);
  }

  @Test
  void shouldPublishEvent() {
    User user =
        new User(
            UUID.randomUUID(), "testuser", "test@orasaka.com", true, Set.of("ROLE_USER"), Map.of());
    UserRegisteredEvent event = new UserRegisteredEvent(user, "token123");

    adapter.publish(event);

    verify(rabbitTemplate).convertAndSend("orasaka.identity.events", "user.registered", event);
  }

  @Test
  void shouldThrowWhenEventIsNull() {
    assertThrows(NullPointerException.class, () -> adapter.publish(null));
  }
}
