package com.orasaka.core.support;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InternalImageRequest} compact constructor validation. */
class InternalImageRequestTest {

  @Nested
  @DisplayName("Validation – error paths")
  class Validation {

    @Test
    @DisplayName("throws IAE when prompt is null")
    void throwsOnNullPrompt() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new InternalImageRequest(null, 512, 512, null, null));
    }

    @Test
    @DisplayName("throws IAE when prompt is blank")
    void throwsOnBlankPrompt() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new InternalImageRequest("  ", 512, 512, null, null));
    }
  }

  @Nested
  @DisplayName("Nominal – happy paths")
  class Nominal {

    @Test
    @DisplayName("valid request preserves all fields")
    void validRequest() {
      var req = new InternalImageRequest("draw a cat", 256, 256, null, null);
      assertEquals("draw a cat", req.prompt());
      assertEquals(256, req.width());
    }
  }
}
