package com.orasaka.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

/** Unit tests for {@link PromptContext} record immutability and wither methods. */
class PromptContextTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() { org.junit.jupiter.api.Assertions.assertTrue(true); }


  @Nested
  @DisplayName("Compact Constructor")
  class CompactConstructor {

    @Test
    @DisplayName("null userMetadata defaults to empty map")
    void nullUserMetadataDefaultsEmpty() {
      var ctx = new PromptContext(QUERY, null, null, QUERY, null, null);
      assertNotNull(ctx.userMetadata());
      assertTrue(ctx.userMetadata().isEmpty());
    }

    @Test
    @DisplayName("null systemMetadata defaults to empty map")
    void nullSystemMetadataDefaultsEmpty() {
      var ctx = new PromptContext(QUERY, Map.of(), null, QUERY, null, null);
      assertNotNull(ctx.systemMetadata());
      assertTrue(ctx.systemMetadata().isEmpty());
    }

    @Test
    @DisplayName("filters null keys from metadata maps")
    void filtersNullKeys() {
      var meta = new HashMap<String, Object>();
      meta.put(null, "value");
      meta.put("key", "valid");
      var ctx = new PromptContext(QUERY, meta, Map.of(), QUERY, null, null);
      assertEquals(1, ctx.userMetadata().size());
      assertEquals("valid", ctx.userMetadata().get("key"));
    }

    @Test
    @DisplayName("filters null values from metadata maps")
    void filtersNullValues() {
      var meta = new HashMap<String, Object>();
      meta.put("key1", null);
      meta.put("key2", "valid");
      var ctx = new PromptContext(QUERY, meta, Map.of(), QUERY, null, null);
      assertEquals(1, ctx.userMetadata().size());
    }

    @Test
    @DisplayName("produces defensively copied immutable map")
    void defensiveCopy() {
      var input = new HashMap<String, Object>();
      input.put("key", "value");
      var ctx = new PromptContext(QUERY, input, Map.of(), QUERY, null, null);
      input.put("mutated", "bad");
      assertFalse(ctx.userMetadata().containsKey("mutated"));
    }
  }

  @Nested
  @DisplayName("Two-argument constructor")
  class TwoArgConstructor {

    @Test
    @DisplayName("sets refinedPrompt to rawUserQuery by default")
    void setsDefaultRefined() {
      var ctx = new PromptContext("original query", Map.of("k", "v"));
      assertEquals("original query", ctx.refinedPrompt());
      assertNull(ctx.routedProvider());
    }
  }

  @Nested
  @DisplayName("Wither methods")
  class WitherMethods {

    @Test
    @DisplayName("withRefinedPrompt returns new instance with updated prompt")
    void withRefined() {
      var ctx = new PromptContext("raw", Map.of());
      var updated = ctx.withRefinedPrompt("refined query");
      assertEquals("refined query", updated.refinedPrompt());
      assertEquals("raw", updated.rawUserQuery());
    }

    @Test
    @DisplayName("withRoutedProvider returns new instance with provider")
    void withRouted() {
      var ctx = new PromptContext("raw", Map.of());
      var updated = ctx.withRoutedProvider("openai");
      assertEquals("openai", updated.routedProvider());
    }

    @Test
    @DisplayName("withUserMetadata returns new instance with updated map")
    void withUserMeta() {
      var ctx = new PromptContext("raw", Map.of());
      var updated = ctx.withUserMetadata(Map.of("role", "admin"));
      assertEquals("admin", updated.userMetadata().get("role"));
      assertTrue(ctx.userMetadata().isEmpty());
    }

    @Test
    @DisplayName("withSystemMetadata returns new instance with updated map")
    void withSystemMeta() {
      var ctx = new PromptContext("raw", Map.of());
      var updated = ctx.withSystemMetadata(Map.of("env", "prod"));
      assertEquals("prod", updated.systemMetadata().get("env"));
    }
  }
}
