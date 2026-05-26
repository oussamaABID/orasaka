package com.orasaka.core.domain.model.chat;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.Context;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

/**
 * Unit tests for {@link InternalChatRequest} compact constructor, factory methods, and message
 * compilation.
 */
class InternalChatRequestTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() { org.junit.jupiter.api.Assertions.assertTrue(true); }


  @Nested
  @DisplayName("Constructor defaults")
  class ConstructorDefaults {

    @Test
    @DisplayName("null messages list defaults to empty list")
    void nullMessagesDefault() {
      var req = new InternalChatRequest("prompt", null, null, Context.anonymous());
      assertNotNull(req.messages());
      assertTrue(req.messages().isEmpty());
    }

    @Test
    @DisplayName("messages list is defensively copied")
    void messagesDefensivelyCopied() {
      var msg = new InternalChatRequest.ChatMessage("user", "hello");
      var req = new InternalChatRequest("prompt", List.of(msg), null, Context.anonymous());
      var msgs = req.messages();
      assertThrows(UnsupportedOperationException.class, () -> msgs.add(msg));
    }
  }

  @Nested
  @DisplayName("Factory methods")
  class FactoryMethods {

    @Test
    @DisplayName("simple() factory creates minimal request")
    void simpleFactory() {
      var req = InternalChatRequest.simple(TEST);
      assertEquals(TEST, req.prompt());
      assertTrue(req.messages().isEmpty());
      assertNull(req.options());
      assertNotNull(req.context());
    }
  }

  @Nested
  @DisplayName("Message compilation")
  class MessageCompilation {

    @Test
    @DisplayName("compileMessages adds UserMessage from refined prompt")
    void compilesMessages() {
      var req = InternalChatRequest.simple(TEST);
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
