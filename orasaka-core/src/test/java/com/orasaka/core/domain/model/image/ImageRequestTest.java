package com.orasaka.core.domain.model.image;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ImageRequest} record — simple accessor coverage. */
class ImageRequestTest {

  @Test
  @DisplayName("record preserves all fields")
  void preservesFields() {
    var ctx = Context.anonymous();
    var req = new ImageRequest("draw a cat", 512, 512, null, null, ctx);
    assertEquals("draw a cat", req.prompt());
    assertEquals(512, req.width());
    assertEquals(512, req.height());
    assertNull(req.model());
    assertTrue(req.settings().isEmpty());
    assertNotNull(req.context());
  }

  @Test
  @DisplayName("null prompt throws NullPointerException")
  void nullPromptThrowsException() {
    var ctx = Context.anonymous();
    assertThrows(
        NullPointerException.class, () -> new ImageRequest(null, null, null, null, null, ctx));
  }

  @Test
  @DisplayName("null context throws NullPointerException [ERR-116]")
  void nullContextThrowsException() {
    assertThrows(
        NullPointerException.class,
        () -> new ImageRequest("draw a cat", 512, 512, null, null, null));
  }
}
