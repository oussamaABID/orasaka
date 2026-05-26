package com.orasaka.core.domain.model.image;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ImageResponse} record — simple accessor coverage. */
class ImageResponseTest {

  @Test
  @DisplayName("record preserves all fields")
  void preservesFields() {
    var data = new byte[] {1, 2, 3};
    var resp = new ImageResponse(data, "https://img.url/cat.png", "png");
    assertArrayEquals(data, resp.imageData());
    assertEquals("https://img.url/cat.png", resp.url());
    assertEquals("png", resp.format());
  }

  @Test
  @DisplayName("null fields are accepted")
  void nullFieldsAccepted() {
    var resp = new ImageResponse(null, null, null);
    assertNull(resp.imageData());
    assertNull(resp.url());
    assertNull(resp.format());
  }
}
