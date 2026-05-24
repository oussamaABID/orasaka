package com.orasaka.core.support;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for support records: InternalChatRequest, OrasakaContext, DefaultOrasakaOptions. */
class SupportRecordsTest {

  @Nested
  @DisplayName("InternalChatRequest")
  class ChatRequest {

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

    @Test
    @DisplayName("simple() factory creates minimal request")
    void simpleFactory() {
      var req = InternalChatRequest.simple("test");
      assertEquals("test", req.prompt());
      assertTrue(req.messages().isEmpty());
      assertNull(req.options());
      assertNull(req.context());
    }

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
  @DisplayName("InternalChatRequest.ChatMessage")
  class ChatMessageRecord {

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

  @Nested
  @DisplayName("OrasakaContext")
  class ContextRecord {

    @Test
    @DisplayName("null preferences defaults to empty map")
    void nullPrefsDefault() {
      var ctx = new OrasakaContext("user1", "conv1", null, null);
      assertNotNull(ctx.preferences());
      assertTrue(ctx.preferences().isEmpty());
    }

    @Test
    @DisplayName("null authorities defaults to empty set")
    void nullAuthoritiesDefault() {
      var ctx = new OrasakaContext("user1", "conv1", null, null);
      assertNotNull(ctx.authorities());
      assertTrue(ctx.authorities().isEmpty());
    }

    @Test
    @DisplayName("hasAuthority matches case-insensitively")
    void hasAuthority() {
      var ctx =
          new OrasakaContext("user1", "conv1", Map.of(), Set.of(new OrasakaAuthority("ADMIN")));
      assertTrue(ctx.hasAuthority("admin"));
      assertTrue(ctx.hasAuthority("ADMIN"));
    }

    @Test
    @DisplayName("hasAuthority returns false when not present")
    void hasAuthorityFalse() {
      var ctx = new OrasakaContext("user1", "conv1", Map.of(), Set.of());
      assertFalse(ctx.hasAuthority("ADMIN"));
    }

    @Test
    @DisplayName("preferences map is immutable")
    void preferencesImmutable() {
      var ctx = new OrasakaContext("u", "c", Map.of("k", "v"), Set.of());
      assertThrows(UnsupportedOperationException.class, () -> ctx.preferences().put("x", "y"));
    }
  }

  @Nested
  @DisplayName("DefaultOrasakaOptions")
  class OptionsRecord {

    @Test
    @DisplayName("no-arg constructor creates empty instance")
    void noArgConstructor() {
      var opts = new DefaultOrasakaOptions();
      assertNull(opts.temperature());
      assertNull(opts.maxTokens());
      assertTrue(opts.extraOptions().isEmpty());
    }

    @Test
    @DisplayName("null extraOptions defaults to empty map")
    void nullExtraDefault() {
      var opts = new DefaultOrasakaOptions(0.5, 100, null);
      assertNotNull(opts.extraOptions());
      assertTrue(opts.extraOptions().isEmpty());
    }

    @Test
    @DisplayName("null keys/values are filtered from extraOptions")
    void filtersNullEntries() {
      var extra = new java.util.HashMap<String, Object>();
      extra.put(null, "v");
      extra.put("k", null);
      extra.put("valid", "ok");
      var opts = new DefaultOrasakaOptions(null, null, extra);
      assertEquals(1, opts.extraOptions().size());
      assertEquals("ok", opts.extraOptions().get("valid"));
    }

    @Test
    @DisplayName("withOption returns new instance with added entry")
    void withOption() {
      var opts = new DefaultOrasakaOptions(0.5, null, Map.of());
      var updated = opts.withOption("key", "value");
      assertEquals("value", updated.getExtraOptions().get("key"));
      assertTrue(opts.extraOptions().isEmpty());
    }

    @Test
    @DisplayName("builder() factory returns empty instance")
    void builderFactory() {
      var opts = DefaultOrasakaOptions.builder();
      assertNotNull(opts);
      assertNull(opts.temperature());
    }

    @Test
    @DisplayName("getTemperature delegates to record accessor")
    void getTemperature() {
      var opts = new DefaultOrasakaOptions(0.8, null, null);
      assertEquals(0.8, opts.getTemperature());
    }

    @Test
    @DisplayName("getMaxTokens delegates to record accessor")
    void getMaxTokens() {
      var opts = new DefaultOrasakaOptions(null, 2048, null);
      assertEquals(2048, opts.getMaxTokens());
    }
  }

  @Nested
  @DisplayName("InternalImageRequest")
  class ImageRequest {

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

    @Test
    @DisplayName("valid request preserves all fields")
    void validRequest() {
      var req = new InternalImageRequest("draw a cat", 256, 256, null, null);
      assertEquals("draw a cat", req.prompt());
      assertEquals(256, req.width());
    }
  }
}
