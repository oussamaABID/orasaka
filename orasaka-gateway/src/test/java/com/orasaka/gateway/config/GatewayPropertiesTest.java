package com.orasaka.gateway.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GatewayProperties} and nested config records. */
class GatewayPropertiesTest {

  @Test
  @DisplayName("CorsProperties stores allowedOrigins")
  void corsProperties() {
    var cors = new GatewayProperties.CorsProperties("http://localhost:3000");
    assertEquals("http://localhost:3000", cors.allowedOrigins());
  }

  @Test
  @DisplayName("SecurityProperties stores dev bypass config")
  void securityProperties() {
    var sec = new GatewayProperties.SecurityProperties("dev-user-id", true);
    assertEquals("dev-user-id", sec.devBypassId());
    assertTrue(sec.devBypassEnabled());
  }

  @Test
  @DisplayName("GatewayProperties composes CORS and security")
  void composedProperties() {
    var cors = new GatewayProperties.CorsProperties("*");
    var sec = new GatewayProperties.SecurityProperties(null, false);
    var props = new GatewayProperties(cors, sec);
    assertNotNull(props.cors());
    assertNotNull(props.security());
    assertFalse(props.security().devBypassEnabled());
  }

  @Test
  @DisplayName("null CORS and security accepted")
  void nullsAccepted() {
    var props = new GatewayProperties(null, null);
    assertNull(props.cors());
    assertNull(props.security());
  }
}
