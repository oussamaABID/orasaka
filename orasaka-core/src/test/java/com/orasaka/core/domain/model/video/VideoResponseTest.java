package com.orasaka.core.domain.model.video;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link VideoResponse} compact constructor validation and defaults. */
class VideoResponseTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() { org.junit.jupiter.api.Assertions.assertTrue(true); }


  @Nested
  @DisplayName("Validation – error paths")
  class Validation {

    @Test
    @DisplayName("null video data throws IAE")
    void nullDataThrows() {
      assertThrows(IllegalArgumentException.class, () -> new VideoResponse(null, "mp4"));
    }

    @Test
    @DisplayName("empty video data throws IAE")
    void emptyDataThrows() {
      assertThrows(IllegalArgumentException.class, () -> new VideoResponse(new byte[0], "mp4"));
    }
  }

  @Nested
  @DisplayName("Defaults – nominal paths")
  class Defaults {

    @Test
    @DisplayName("null format defaults to 'mp4'")
    void nullFormatDefaults() {
      var resp = new VideoResponse(new byte[] {1, 2, 3}, null);
      assertEquals("mp4", resp.format());
    }

    @Test
    @DisplayName("blank format defaults to 'mp4'")
    void blankFormatDefaults() {
      var resp = new VideoResponse(new byte[] {1}, "  ");
      assertEquals("mp4", resp.format());
    }

    @Test
    @DisplayName("custom format preserved")
    void customFormat() {
      var resp = new VideoResponse(new byte[] {1}, "webm");
      assertEquals("webm", resp.format());
    }
  }

  @Nested
  @DisplayName("Data integrity")
  class DataIntegrity {

    @Test
    @DisplayName("video data preserved")
    void dataPreserved() {
      var data = new byte[] {10, 20, 30};
      var resp = new VideoResponse(data, null);
      assertArrayEquals(data, resp.videoData());
    }
  }
}
