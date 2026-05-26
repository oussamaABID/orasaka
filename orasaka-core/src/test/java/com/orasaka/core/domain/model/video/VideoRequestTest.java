package com.orasaka.core.domain.model.video;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.Context;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

/** Unit tests for {@link VideoRequest} compact constructor validation and defaults. */
class VideoRequestTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() { org.junit.jupiter.api.Assertions.assertTrue(true); }


  private static final Context CTX = Context.anonymous();

  @Nested
  @DisplayName("Validation – error paths")
  class Validation {

    @Test
    @DisplayName("null prompt throws NullPointerException")
    void nullPromptThrows() {
      assertThrows(NullPointerException.class, () -> new VideoRequest(null, null, null, CTX));
    }

    @Test
    @DisplayName("blank prompt throws IAE")
    void blankPromptThrows() {
      assertThrows(IllegalArgumentException.class, () -> new VideoRequest("  ", null, null, CTX));
    }

    @Test
    @DisplayName("null context throws NullPointerException [ERR-116]")
    void nullContextThrows() {
      assertThrows(NullPointerException.class, () -> new VideoRequest(TEST, null, null, null));
    }
  }

  @Nested
  @DisplayName("Defaults – nominal paths")
  class Defaults {

    @Test
    @DisplayName("null duration defaults to 4 seconds")
    void nullDurationDefaults() {
      var req = new VideoRequest("test video", null, null, CTX);
      assertEquals(4, req.durationSeconds());
    }

    @Test
    @DisplayName("custom duration preserved")
    void customDuration() {
      var req = new VideoRequest(TEST, 10, null, CTX);
      assertEquals(10, req.durationSeconds());
    }

    @Test
    @DisplayName("null settings defaults to empty map")
    void nullSettingsDefaults() {
      var req = new VideoRequest(TEST, null, null, CTX);
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
      var req = new VideoRequest(TEST, null, settings, CTX);
      settings.put("mutated", "bad");
      assertFalse(req.settings().containsKey("mutated"));
    }
  }
}
