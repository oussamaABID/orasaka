package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OrasakaFeaturesProperties} compact constructor validation and immutability.
 */
class OrasakaFeaturesPropertiesTest {

  @Nested
  @DisplayName("Compact constructor")
  class CompactConstructor {

    @Test
    @DisplayName("accepts null features map and defaults to empty")
    void defaultsToEmptyWhenNull() {
      var props = new OrasakaFeaturesProperties(null);
      assertNotNull(props.features());
      assertTrue(props.features().isEmpty());
    }

    @Test
    @DisplayName("defensively copies the features map")
    void defensivelyCopiesMap() {
      var config =
          new OrasakaFeaturesProperties.FeatureConfig(
              true, "Chat", "💬", "/api/chat", "POST", null);
      var mutable = new java.util.HashMap<>(Map.of("chat", config));
      var props = new OrasakaFeaturesProperties(mutable);

      mutable.put("injected", config);
      assertFalse(props.features().containsKey("injected"));
    }

    @Test
    @DisplayName("resulting map is immutable")
    void resultingMapIsImmutable() {
      var config =
          new OrasakaFeaturesProperties.FeatureConfig(
              true, "Chat", "💬", "/api/chat", "POST", null);
      var props = new OrasakaFeaturesProperties(Map.of("chat", config));

      assertThrows(UnsupportedOperationException.class, () -> props.features().put("x", config));
    }
  }

  @Nested
  @DisplayName("FeatureConfig validation")
  class FeatureConfigValidation {

    @Test
    @DisplayName("throws when label is null")
    void throwsWhenLabelNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new OrasakaFeaturesProperties.FeatureConfig(true, null, "📦", "/api", "POST", null));
    }

    @Test
    @DisplayName("throws when label is blank")
    void throwsWhenLabelBlank() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new OrasakaFeaturesProperties.FeatureConfig(true, "  ", "📦", "/api", "POST", null));
    }

    @Test
    @DisplayName("throws when icon is null")
    void throwsWhenIconNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new OrasakaFeaturesProperties.FeatureConfig(
                  true, "Chat", null, "/api", "POST", null));
    }

    @Test
    @DisplayName("throws when icon is blank")
    void throwsWhenIconBlank() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new OrasakaFeaturesProperties.FeatureConfig(
                  true, "Chat", "  ", "/api", "POST", null));
    }

    @Test
    @DisplayName("throws when uriPath is null")
    void throwsWhenUriPathNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new OrasakaFeaturesProperties.FeatureConfig(true, "Chat", "💬", null, "POST", null));
    }

    @Test
    @DisplayName("throws when httpMethod is null")
    void throwsWhenHttpMethodNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new OrasakaFeaturesProperties.FeatureConfig(true, "Chat", "💬", "/api", null, null));
    }

    @Test
    @DisplayName("accepts valid feature config with null payload template")
    void acceptsValidConfig() {
      var config =
          new OrasakaFeaturesProperties.FeatureConfig(
              true, "Chat", "💬", "/api/chat", "POST", null);
      assertTrue(config.enabled());
      assertEquals("Chat", config.label());
      assertNull(config.payloadTemplate());
    }
  }
}
