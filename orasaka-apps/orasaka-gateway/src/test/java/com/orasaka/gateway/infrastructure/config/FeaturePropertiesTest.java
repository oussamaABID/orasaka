package com.orasaka.gateway.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FeaturePropertiesTest {

  @Test
  void validConstruction_setsAllFields() {
    var fp = new FeatureProperties(true, "Image Gen", "🖼️", "/api/v1/images", "POST", "{}");
    assertTrue(fp.enabled());
    assertEquals("Image Gen", fp.label());
    assertEquals("🖼️", fp.icon());
    assertEquals("/api/v1/images", fp.uriPath());
    assertEquals("POST", fp.httpMethod());
    assertEquals("{}", fp.payloadTemplate());
  }

  @Test
  void nullLabel_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new FeatureProperties(true, null, "icon", "/path", "POST", "{}"));
  }

  @Test
  void nullIcon_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new FeatureProperties(true, "label", null, "/path", "POST", "{}"));
  }

  @Test
  void nullUriPath_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new FeatureProperties(true, "label", "icon", null, "POST", "{}"));
  }

  @Test
  void nullHttpMethod_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new FeatureProperties(true, "label", "icon", "/path", null, "{}"));
  }

  @Test
  void nullPayloadTemplate_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new FeatureProperties(true, "label", "icon", "/path", "POST", null));
  }
}
