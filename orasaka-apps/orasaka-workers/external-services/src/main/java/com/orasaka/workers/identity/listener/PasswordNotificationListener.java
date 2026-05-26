package com.orasaka.workers.identity.listener;

import com.orasaka.identity.domain.model.PasswordResetRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes password reset requested events and simulates sending recovery email or SMS. */
@Component
public class PasswordNotificationListener {

  private static final Logger logger = LoggerFactory.getLogger(PasswordNotificationListener.class);

  @RabbitListener(queues = "orasaka.workers.identity.password")
  public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
    logger.info(
        "Notification Listener received password reset request for email={}.", event.email());

    // Simulate email dispatch
    logger.info("Password reset recovery link sent to email={}", event.email());
  }
}
