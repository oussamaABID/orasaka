package com.orasaka.core.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link UserContextResolver} — user metadata enrichment from security context. */
class UserContextResolverTest {

  @Nested
  @DisplayName("Security context resolution")
  class SecurityResolution {

    @Test
    @DisplayName("returns context unchanged when no security metadata is available")
    void emptySecurityContext() {
      var resolver = new UserContextResolver();
      var ctx = new PromptContext("hello", Map.of("existing", "data"));

      var result = resolver.intercept(ctx);
      // Without Spring Security configured, SecurityContextUtil returns empty map
      assertEquals(ctx.userMetadata(), result.userMetadata());
    }

    @Test
    @DisplayName("preserves existing user metadata keys")
    void preservesExistingMetadata() {
      var resolver = new UserContextResolver();
      var ctx = new PromptContext("hello", Map.of("userId", "u1", "tier", "premium"));

      var result = resolver.intercept(ctx);
      assertTrue(result.userMetadata().containsKey("userId"));
      assertTrue(result.userMetadata().containsKey("tier"));
    }
  }

  @Test
  @DisplayName("getOrder returns 1")
  void orderIs1() {
    var resolver = new UserContextResolver();
    assertEquals(1, resolver.getOrder());
  }
}
