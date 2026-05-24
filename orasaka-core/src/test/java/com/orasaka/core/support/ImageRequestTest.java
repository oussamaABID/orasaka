package com.orasaka.core.support;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ImageRequest} record — simple accessor coverage. */
class ImageRequestTest {

  @Test
  @DisplayName("record preserves all fields")
  void preservesFields() {
    var req = new ImageRequest("draw a cat", 512, 512, null, null);
    assertEquals("draw a cat", req.prompt());
    assertEquals(512, req.width());
    assertEquals(512, req.height());
    assertNull(req.options());
    assertNull(req.context());
  }

  @Test
  @DisplayName("null fields are accepted")
  void nullFieldsAccepted() {
    var req = new ImageRequest(null, null, null, null, null);
    assertNull(req.prompt());
    assertNull(req.width());
  }
}
