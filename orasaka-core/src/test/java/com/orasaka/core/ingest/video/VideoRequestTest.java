package com.orasaka.core.ingest.video;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link VideoRequest} compact constructor validation and defaults. */
class VideoRequestTest {

  @Nested
  @DisplayName("Validation – error paths")
  class Validation {

    @Test
    @DisplayName("null prompt throws IAE")
    void nullPromptThrows() {
      assertThrows(IllegalArgumentException.class, () -> new VideoRequest(null, null, null, null));
    }

    @Test
    @DisplayName("blank prompt throws IAE")
    void blankPromptThrows() {
      assertThrows(IllegalArgumentException.class, () -> new VideoRequest("  ", null, null, null));
    }
  }

  @Nested
  @DisplayName("Defaults – nominal paths")
  class Defaults {

    @Test
    @DisplayName("null duration defaults to 4 seconds")
    void nullDurationDefaults() {
      var req = new VideoRequest("test video", null, null, null);
      assertEquals(4, req.durationSeconds());
    }

    @Test
    @DisplayName("custom duration preserved")
    void customDuration() {
      var req = new VideoRequest("test", 10, null, null);
      assertEquals(10, req.durationSeconds());
    }

    @Test
    @DisplayName("null settings defaults to empty map")
    void nullSettingsDefaults() {
      var req = new VideoRequest("test", null, null, null);
      assertNotNull(req.settings());
      assertTrue(req.settings().isEmpty());
    }
  }

  @Nested
  @DisplayName("Defensive copying")
  class DefensiveCopying {

    @Test
    @DisplayName("settings are defensively copied")
    void settingsDefensivelyCopied() {
      Map<String, Object> settings = new HashMap<>();
      settings.put("key", "value");
      var req = new VideoRequest("test", null, settings, null);
      settings.put("mutated", "bad");
      assertFalse(req.settings().containsKey("mutated"));
    }
  }
}
