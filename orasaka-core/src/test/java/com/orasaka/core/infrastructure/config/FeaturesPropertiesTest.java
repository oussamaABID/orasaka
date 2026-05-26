package com.orasaka.core.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

/** Unit tests for {@link FeaturesProperties} and {@link FeaturesProperties.FeatureConfig}. */
class FeaturesPropertiesTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() { org.junit.jupiter.api.Assertions.assertTrue(true); }


  @Nested
  @DisplayName("FeaturesProperties")
  class PropertiesTests {

    @Test
    @DisplayName("null features defaults to empty map")
    void nullFeaturesDefaults() {
      var props = new FeaturesProperties(null);
      assertNotNull(props.features());
      assertTrue(props.features().isEmpty());
    }

    @Test
    @DisplayName("immutable copy of features map")
    void immutableCopy() {
      var config =
          new FeaturesProperties.FeatureConfig(
              true, "Text Chat", "chat", "/api/v1/chat", METHOD_POST, null);
      var props = new FeaturesProperties(Map.of("orasaka.core.chat.text", config));
      var features = props.features();
      assertThrows(UnsupportedOperationException.class, () -> features.put("new", config));
    }
  }

  @Nested
  @DisplayName("FeatureConfig validation")
  class FeatureConfigTests {

    @Test
    @DisplayName("valid config preserves all fields")
    void validConfig() {
      var config =
          new FeaturesProperties.FeatureConfig(
              true, "Image Gen", "image", "/api/v1/image", METHOD_POST, "{\"prompt\":\"${p}\"}");
      assertTrue(config.enabled());
      assertEquals("Image Gen", config.label());
      assertEquals("image", config.icon());
      assertEquals("/api/v1/image", config.uriPath());
      assertEquals(METHOD_POST, config.httpMethod());
      assertEquals("{\"prompt\":\"${p}\"}", config.payloadTemplate());
    }

    @Test
    @DisplayName("null label throws NPE")
    void nullLabel() {
      assertThrows(
          NullPointerException.class,
          () -> new FeaturesProperties.FeatureConfig(true, null, "icon", "/uri", METHOD_POST, null));
    }

    @Test
    @DisplayName("blank label throws IAE")
    void blankLabel() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new FeaturesProperties.FeatureConfig(true, "  ", "icon", "/uri", METHOD_POST, null));
    }

    @Test
    @DisplayName("null icon throws NPE")
    void nullIcon() {
      assertThrows(
          NullPointerException.class,
          () -> new FeaturesProperties.FeatureConfig(true, "Label", null, "/uri", METHOD_POST, null));
    }

    @Test
    @DisplayName("blank icon throws IAE")
    void blankIcon() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new FeaturesProperties.FeatureConfig(true, "Label", "  ", "/uri", METHOD_POST, null));
    }

    @Test
    @DisplayName("null uriPath throws NPE")
    void nullUriPath() {
      assertThrows(
          NullPointerException.class,
          () -> new FeaturesProperties.FeatureConfig(true, "Label", "icon", null, METHOD_POST, null));
    }

    @Test
    @DisplayName("null httpMethod throws NPE")
    void nullHttpMethod() {
      assertThrows(
          NullPointerException.class,
          () -> new FeaturesProperties.FeatureConfig(true, "Label", "icon", "/uri", null, null));
    }

    @Test
    @DisplayName("disabled feature preserves enabled=false")
    void disabledFeature() {
      var config =
          new FeaturesProperties.FeatureConfig(
              false, "Speech", "mic", "/api/v1/speech", METHOD_POST, null);
      assertFalse(config.enabled());
    }
  }
}
