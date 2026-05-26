package com.orasaka.identity.domain.ports.outbound;

import com.orasaka.identity.domain.model.UserRegisteredEvent;

/** Outbound port interface for publishing user-related identity events. */
public interface UserEventPublisher {

  /**
   * Publishes the user registered event downstream.
   *
   * @param event the UserRegisteredEvent to publish
   */
  void publish(UserRegisteredEvent event);
}
