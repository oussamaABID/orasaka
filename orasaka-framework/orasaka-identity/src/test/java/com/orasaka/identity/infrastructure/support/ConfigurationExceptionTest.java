package com.orasaka.identity.infrastructure.support;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConfigurationExceptionTest {

  @Test
  void constructWithMessage_carriesMessage() {
    var ex = new ConfigurationException("Missing schema");
    assertEquals("Missing schema", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }
}
