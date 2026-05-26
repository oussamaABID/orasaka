package com.orasaka.core.domain.model.video;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class VideoResponseTest {

  @Test
  void validConstruction() {
    var response = new VideoResponse(new byte[] {1, 2, 3}, "mp4", "/out.mp4", Map.of("fps", 30));
    assertArrayEquals(new byte[] {1, 2, 3}, response.videoData());
    assertEquals("mp4", response.format());
    assertEquals("/out.mp4", response.outputPath());
    assertEquals(30, response.metrics().get("fps"));
  }

  @Test
  void nullFormat_defaultsToMp4() {
    var response = new VideoResponse(new byte[] {1}, null, null, null);
    assertEquals("mp4", response.format());
    assertEquals("", response.outputPath());
    assertTrue(response.metrics().isEmpty());
  }

  @Test
  void shortConstructor_2args() {
    var response = new VideoResponse(new byte[] {1}, "webm");
    assertEquals("webm", response.format());
    assertEquals("", response.outputPath());
    assertTrue(response.metrics().isEmpty());
  }

  @Test
  void shortConstructor_3args() {
    var response = new VideoResponse(new byte[] {1}, "mp4", Map.of("key", "val"));
    assertEquals("", response.outputPath());
    assertEquals("val", response.metrics().get("key"));
  }

  @Test
  void nullVideoData_throws() {
    assertThrows(IllegalArgumentException.class, () -> new VideoResponse(null, "mp4"));
  }

  @Test
  void emptyVideoData_throws() {
    assertThrows(IllegalArgumentException.class, () -> new VideoResponse(new byte[0], "mp4"));
  }

  @Test
  void equality_withSameData() {
    var r1 = new VideoResponse(new byte[] {1, 2}, "mp4");
    var r2 = new VideoResponse(new byte[] {1, 2}, "mp4");
    assertEquals(r1, r2);
    assertEquals(r1.hashCode(), r2.hashCode());
  }

  @Test
  void toString_showsSize() {
    var response = new VideoResponse(new byte[] {1, 2, 3}, "mp4");
    assertTrue(response.toString().contains("3 bytes"));
  }
}
