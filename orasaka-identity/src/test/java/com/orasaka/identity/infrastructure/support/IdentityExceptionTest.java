package com.orasaka.identity.infrastructure.support;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for identity exception hierarchy. */
class IdentityExceptionTest {

  @Test
  @DisplayName("InvalidRequestException carries message")
  void invalidRequestMessage() {
    var ex = new InvalidRequestException("bad input");
    assertEquals("bad input", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  @DisplayName("UserAlreadyExistsException carries message")
  void userAlreadyExistsMessage() {
    var ex = new UserAlreadyExistsException("email taken");
    assertEquals("email taken", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }
}
