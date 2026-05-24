package com.orasaka.core.support;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SpeechRequest} record — simple accessor coverage. */
class SpeechRequestTest {

  @Test
  @DisplayName("record preserves all fields")
  void preservesFields() {
    var req = new SpeechRequest("Hello world", null, null);
    assertEquals("Hello world", req.text());
    assertNull(req.options());
    assertNull(req.context());
  }

  @Test
  @DisplayName("null fields are accepted")
  void nullFieldsAccepted() {
    var req = new SpeechRequest(null, null, null);
    assertNull(req.text());
  }
}
