package com.orasaka.gateway.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FeatureRegistryPropertiesTest {

  @Test
  void validConstruction_setsFeatures() {
    var feature = new FeatureProperties(true, "Chat", "💬", "/api/v1/chat", "POST", "{}");
    var props = new FeatureRegistryProperties(Map.of("chat", feature));
    assertEquals(1, props.features().size());
    assertEquals(feature, props.features().get("chat"));
  }

  @Test
  void nullFeatures_throws() {
    assertThrows(NullPointerException.class, () -> new FeatureRegistryProperties(null));
  }

  @Test
  void emptyFeatures_allowed() {
    var props = new FeatureRegistryProperties(Map.of());
    assertTrue(props.features().isEmpty());
  }
}
