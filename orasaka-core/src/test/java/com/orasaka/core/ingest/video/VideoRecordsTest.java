package com.orasaka.core.ingest.video;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for video request/response records covering compact constructor validation. */
class VideoRecordsTest {

  @Nested
  @DisplayName("OrasakaVideoRequest")
  class VideoRequest {

    @Test
    @DisplayName("null prompt throws IAE")
    void nullPromptThrows() {
      assertThrows(
          IllegalArgumentException.class, () -> new OrasakaVideoRequest(null, null, null, null));
    }

    @Test
    @DisplayName("blank prompt throws IAE")
    void blankPromptThrows() {
      assertThrows(
          IllegalArgumentException.class, () -> new OrasakaVideoRequest("  ", null, null, null));
    }

    @Test
    @DisplayName("null duration defaults to 4 seconds")
    void nullDurationDefaults() {
      var req = new OrasakaVideoRequest("test video", null, null, null);
      assertEquals(4, req.durationSeconds());
    }

    @Test
    @DisplayName("custom duration preserved")
    void customDuration() {
      var req = new OrasakaVideoRequest("test", 10, null, null);
      assertEquals(10, req.durationSeconds());
    }

    @Test
    @DisplayName("null settings defaults to empty map")
    void nullSettingsDefaults() {
      var req = new OrasakaVideoRequest("test", null, null, null);
      assertNotNull(req.settings());
      assertTrue(req.settings().isEmpty());
    }

    @Test
    @DisplayName("settings are defensively copied")
    void settingsDefensivelyCopied() {
      var settings = new java.util.HashMap<String, Object>();
      settings.put("key", "value");
      var req = new OrasakaVideoRequest("test", null, settings, null);
      settings.put("mutated", "bad");
      assertFalse(req.settings().containsKey("mutated"));
    }
  }

  @Nested
  @DisplayName("OrasakaVideoResponse")
  class VideoResponse {

    @Test
    @DisplayName("null video data throws IAE")
    void nullDataThrows() {
      assertThrows(IllegalArgumentException.class, () -> new OrasakaVideoResponse(null, "mp4"));
    }

    @Test
    @DisplayName("empty video data throws IAE")
    void emptyDataThrows() {
      assertThrows(
          IllegalArgumentException.class, () -> new OrasakaVideoResponse(new byte[0], "mp4"));
    }

    @Test
    @DisplayName("null format defaults to 'mp4'")
    void nullFormatDefaults() {
      var resp = new OrasakaVideoResponse(new byte[] {1, 2, 3}, null);
      assertEquals("mp4", resp.format());
    }

    @Test
    @DisplayName("blank format defaults to 'mp4'")
    void blankFormatDefaults() {
      var resp = new OrasakaVideoResponse(new byte[] {1}, "  ");
      assertEquals("mp4", resp.format());
    }

    @Test
    @DisplayName("custom format preserved")
    void customFormat() {
      var resp = new OrasakaVideoResponse(new byte[] {1}, "webm");
      assertEquals("webm", resp.format());
    }

    @Test
    @DisplayName("video data preserved")
    void dataPreserved() {
      var data = new byte[] {10, 20, 30};
      var resp = new OrasakaVideoResponse(data, null);
      assertArrayEquals(data, resp.videoData());
    }
  }
}
