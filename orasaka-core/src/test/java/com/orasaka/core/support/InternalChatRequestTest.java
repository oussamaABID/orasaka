package com.orasaka.core.support;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InternalChatRequest} compact constructor, factory methods, and message
 * compilation.
 */
class InternalChatRequestTest {

  @Nested
  @DisplayName("Constructor defaults")
  class ConstructorDefaults {

    @Test
    @DisplayName("null messages list defaults to empty list")
    void nullMessagesDefault() {
      var req = new InternalChatRequest("prompt", null, null, null);
      assertNotNull(req.messages());
      assertTrue(req.messages().isEmpty());
    }

    @Test
    @DisplayName("messages list is defensively copied")
    void messagesDefensivelyCopied() {
      var msg = new InternalChatRequest.ChatMessage("user", "hello");
      var req = new InternalChatRequest("prompt", List.of(msg), null, null);
      assertThrows(UnsupportedOperationException.class, () -> req.messages().add(msg));
    }
  }

  @Nested
  @DisplayName("Factory methods")
  class FactoryMethods {

    @Test
    @DisplayName("simple() factory creates minimal request")
    void simpleFactory() {
      var req = InternalChatRequest.simple("test");
      assertEquals("test", req.prompt());
      assertTrue(req.messages().isEmpty());
      assertNull(req.options());
      assertNull(req.context());
    }
  }

  @Nested
  @DisplayName("Message compilation")
  class MessageCompilation {

    @Test
    @DisplayName("compileMessages adds UserMessage from refined prompt")
    void compilesMessages() {
      var req = InternalChatRequest.simple("test");
      var messages =
          req.compileMessages(
              "refined",
              msg -> new org.springframework.ai.chat.messages.UserMessage(msg.content()));
      assertFalse(messages.isEmpty());
    }
  }

  @Nested
  @DisplayName("ChatMessage record")
  class ChatMessageTests {

    @Test
    @DisplayName("throws IAE when role is null")
    void throwsOnNullRole() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new InternalChatRequest.ChatMessage(null, "content"));
    }

    @Test
    @DisplayName("throws IAE when role is blank")
    void throwsOnBlankRole() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new InternalChatRequest.ChatMessage("  ", "content"));
    }

    @Test
    @DisplayName("null content defaults to empty string")
    void nullContentDefault() {
      var msg = new InternalChatRequest.ChatMessage("user", null);
      assertEquals("", msg.content());
    }
  }
}
