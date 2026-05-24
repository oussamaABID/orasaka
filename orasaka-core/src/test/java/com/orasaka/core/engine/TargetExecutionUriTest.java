package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TargetExecutionUri} — compact constructor validation. */
class TargetExecutionUriTest {

  @Nested
  @DisplayName("Constructor invariants")
  class Invariants {

    @Test
    @DisplayName("null uriPath throws NPE")
    void nullUriPath() {
      assertThrows(NullPointerException.class, () -> new TargetExecutionUri(null, "GET", null));
    }

    @Test
    @DisplayName("blank uriPath throws IAE")
    void blankUriPath() {
      assertThrows(IllegalArgumentException.class, () -> new TargetExecutionUri("  ", "GET", null));
    }

    @Test
    @DisplayName("null httpMethod throws NPE")
    void nullHttpMethod() {
      assertThrows(
          NullPointerException.class, () -> new TargetExecutionUri("/api/chat", null, null));
    }

    @Test
    @DisplayName("blank httpMethod throws IAE")
    void blankHttpMethod() {
      assertThrows(
          IllegalArgumentException.class, () -> new TargetExecutionUri("/api/chat", "  ", null));
    }
  }

  @Nested
  @DisplayName("Valid construction")
  class ValidConstruction {

    @Test
    @DisplayName("preserves all fields")
    void preservesFields() {
      var uri = new TargetExecutionUri("/api/v1/chat", "POST", "{\"prompt\":\"${p}\"}");
      assertEquals("/api/v1/chat", uri.uriPath());
      assertEquals("POST", uri.httpMethod());
      assertEquals("{\"prompt\":\"${p}\"}", uri.payloadTemplate());
    }

    @Test
    @DisplayName("null payloadTemplate accepted")
    void nullPayloadTemplate() {
      var uri = new TargetExecutionUri("/api/v1/chat", "GET", null);
      assertNull(uri.payloadTemplate());
    }
  }
}
