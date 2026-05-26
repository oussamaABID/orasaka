package com.orasaka.core.domain.model.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ChatResponseTest {

  @Test
  void validConstruction() {
    var response = new ChatResponse("Hello!", "conv-1", Map.of("provider", "openai"));
    assertEquals("Hello!", response.content());
    assertEquals("conv-1", response.conversationId());
    assertEquals("openai", response.metadata().get("provider"));
  }

  @Test
  void nullFields_allowed() {
    var response = new ChatResponse(null, null, null);
    assertNull(response.content());
    assertNull(response.conversationId());
    assertNull(response.metadata());
  }
}
