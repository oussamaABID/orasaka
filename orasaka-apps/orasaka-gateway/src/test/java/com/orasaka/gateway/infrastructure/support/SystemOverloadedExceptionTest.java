package com.orasaka.gateway.infrastructure.support;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SystemOverloadedExceptionTest {

  @Test
  void testConstructorWithMessage() {
    var ex = new SystemOverloadedException("test message");
    assertEquals("test message", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testConstructorWithMessageAndCause() {
    var cause = new RuntimeException("root cause");
    var ex = new SystemOverloadedException("test message", cause);
    assertEquals("test message", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }
}
