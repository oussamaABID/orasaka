package com.orasaka.gateway.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RateLimitPropertiesTest {

  @Test
  void validConstruction_enabled() {
    var props = new RateLimitProperties(true, "free");
    assertTrue(props.enabled());
    assertEquals("free", props.defaultTier());
  }

  @Test
  void validConstruction_disabled() {
    var props = new RateLimitProperties(false, "none");
    assertFalse(props.enabled());
    assertEquals("none", props.defaultTier());
  }

  @Test
  void nullDefaultTier_allowed() {
    var props = new RateLimitProperties(true, null);
    assertNull(props.defaultTier());
  }
}
