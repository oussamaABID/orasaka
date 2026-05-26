package com.orasaka.gateway.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GatewayPropertiesTest {

  @Test
  void uploadsConfig_validConstruction() {
    var uc = new GatewayProperties.UploadsConfig("uploads/", "/uploads/**", 3600);
    assertEquals("uploads/", uc.directory());
    assertEquals("/uploads/**", uc.handlerPath());
    assertEquals(3600, uc.cachePeriod());
  }

  @Test
  void uploadsConfig_nullDirectory_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GatewayProperties.UploadsConfig(null, "/uploads/**", 3600));
  }

  @Test
  void uploadsConfig_blankDirectory_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GatewayProperties.UploadsConfig("  ", "/uploads/**", 3600));
  }

  @Test
  void uploadsConfig_nullHandlerPath_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GatewayProperties.UploadsConfig("uploads/", null, 3600));
  }

  @Test
  void uploadsConfig_blankHandlerPath_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GatewayProperties.UploadsConfig("uploads/", "  ", 3600));
  }

  @Test
  void asyncConfig_validConstruction() {
    var ac = new GatewayProperties.AsyncConfig(30000);
    assertEquals(30000, ac.timeoutMs());
  }

  @Test
  void asyncConfig_zeroTimeout_throws() {
    assertThrows(IllegalArgumentException.class, () -> new GatewayProperties.AsyncConfig(0));
  }

  @Test
  void asyncConfig_negativeTimeout_throws() {
    assertThrows(IllegalArgumentException.class, () -> new GatewayProperties.AsyncConfig(-1));
  }

  @Test
  void gatewayProperties_validConstruction() {
    var uploads = new GatewayProperties.UploadsConfig("uploads/", "/uploads/**", 3600);
    var async = new GatewayProperties.AsyncConfig(30000);
    var gp = new GatewayProperties(uploads, async);
    assertEquals(uploads, gp.uploads());
    assertEquals(async, gp.async());
  }
}
