package com.orasaka.workers.identity.listener;

import com.orasaka.identity.domain.model.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes user registration events and simulates sending verification email or SMS. */
@Component
public class IdentityNotificationListener {

  private static final Logger logger = LoggerFactory.getLogger(IdentityNotificationListener.class);

  @RabbitListener(queues = "orasaka.workers.identity.registration")
  public void onUserRegistered(UserRegisteredEvent event) {
    logger.info(
        "Notification Listener received registration for user={}. Plaintext token present: {}",
        event.user().email(),
        event.plaintextToken() != null);

    // Simulate email dispatch
    logger.info("Verification email sent to user={}", event.user().email());
  }
}
