package com.orasaka.identity.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/** Unit tests for {@link IdentityInfrastructureProperties} nested records. */
class IdentityInfrastructurePropertiesTest {

  @Test
  @DisplayName("EmailVerification stores enabled flag")
  void emailVerification() {
    var ev = new IdentityInfrastructureProperties.EmailVerification(true);
    assertTrue(ev.enabled());
  }

  @Test
  @DisplayName("Interceptions stores enabled and schemas map")
  void interceptions() {
    var resource = new ByteArrayResource("{}".getBytes());
    var schemas = Map.of("onboarding", (Resource) resource);
    var inter = new IdentityInfrastructureProperties.Interceptions(true, schemas);
    assertTrue(inter.enabled());
    assertSame(resource, inter.schemas().get("onboarding"));
  }

  @Test
  @DisplayName("IdentityInfrastructureProperties composes sub-records")
  void composedProperties() {
    var ev = new IdentityInfrastructureProperties.EmailVerification(false);
    var inter = new IdentityInfrastructureProperties.Interceptions(false, null);
    var props = new IdentityInfrastructureProperties(ev, inter);
    assertNotNull(props.emailVerification());
    assertNotNull(props.interceptions());
    assertFalse(props.emailVerification().enabled());
  }
}
