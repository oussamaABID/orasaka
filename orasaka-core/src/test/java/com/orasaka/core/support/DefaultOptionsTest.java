package com.orasaka.core.support;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DefaultOptions} — constructor defaults, immutability, and builder. */
class DefaultOptionsTest {

  @Nested
  @DisplayName("Constructor defaults")
  class ConstructorDefaults {

    @Test
    @DisplayName("no-arg constructor creates empty instance")
    void noArgConstructor() {
      var opts = new DefaultOptions();
      assertNull(opts.temperature());
      assertNull(opts.maxTokens());
      assertTrue(opts.extraOptions().isEmpty());
    }

    @Test
    @DisplayName("null extraOptions defaults to empty map")
    void nullExtraDefault() {
      var opts = new DefaultOptions(0.5, 100, null);
      assertNotNull(opts.extraOptions());
      assertTrue(opts.extraOptions().isEmpty());
    }

    @Test
    @DisplayName("null keys/values are filtered from extraOptions")
    void filtersNullEntries() {
      var extra = new HashMap<String, Object>();
      extra.put(null, "v");
      extra.put("k", null);
      extra.put("valid", "ok");
      var opts = new DefaultOptions(null, null, extra);
      assertEquals(1, opts.extraOptions().size());
      assertEquals("ok", opts.extraOptions().get("valid"));
    }
  }

  @Nested
  @DisplayName("Mutation and builder")
  class MutationAndBuilder {

    @Test
    @DisplayName("withOption returns new instance with added entry")
    void withOption() {
      var opts = new DefaultOptions(0.5, null, Map.of());
      var updated = opts.withOption("key", "value");
      assertEquals("value", updated.getExtraOptions().get("key"));
      assertTrue(opts.extraOptions().isEmpty());
    }

    @Test
    @DisplayName("builder() factory returns empty instance")
    void builderFactory() {
      var opts = DefaultOptions.builder();
      assertNotNull(opts);
      assertNull(opts.temperature());
    }
  }

  @Nested
  @DisplayName("Accessor delegates")
  class AccessorDelegates {

    @Test
    @DisplayName("getTemperature delegates to record accessor")
    void getTemperature() {
      var opts = new DefaultOptions(0.8, null, null);
      assertEquals(0.8, opts.getTemperature());
    }

    @Test
    @DisplayName("getMaxTokens delegates to record accessor")
    void getMaxTokens() {
      var opts = new DefaultOptions(null, 2048, null);
      assertEquals(2048, opts.getMaxTokens());
    }
  }
}
