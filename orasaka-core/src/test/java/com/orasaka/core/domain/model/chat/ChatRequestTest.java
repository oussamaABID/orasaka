package com.orasaka.core.domain.model.chat;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ChatRequest} record constructor and defaults. */
class ChatRequestTest {

  @Test
  @DisplayName("record preserves all fields and defaults null messages to empty list")
  void preservesFieldsAndDefaults() {
    var req = new ChatRequest("Hello", null, null, Context.anonymous());
    assertEquals("Hello", req.prompt());
    assertNotNull(req.messages());
    assertTrue(req.messages().isEmpty());
    assertTrue(req.settings().isEmpty());
    assertNotNull(req.context());
  }

  @Test
  @DisplayName("null prompt throws NullPointerException")
  void nullPromptThrowsException() {
    var ctx = Context.anonymous();
    assertThrows(NullPointerException.class, () -> new ChatRequest(null, null, null, ctx));
  }

  @Test
  @DisplayName("null context throws NullPointerException [ERR-116]")
  void nullContextThrowsException() {
    assertThrows(NullPointerException.class, () -> new ChatRequest("Hello", null, null, null));
  }

  @Test
  @DisplayName("simple factory creates request with prompt and empty messages")
  void simpleFactory() {
    var req = ChatRequest.simple("test prompt");
    assertEquals("test prompt", req.prompt());
    assertNotNull(req.messages());
    assertTrue(req.messages().isEmpty());
    assertNotNull(req.context());
  }
}
