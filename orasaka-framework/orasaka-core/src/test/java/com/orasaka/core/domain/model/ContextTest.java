package com.orasaka.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContextTest {

  @Test
  void validConstruction_setsAllFields() {
    var ctx =
        new Context("user-1", "conv-1", Map.of("key", "value"), Set.of(new Authority("ROLE_USER")));
    assertEquals("user-1", ctx.userId());
    assertEquals("conv-1", ctx.conversationId());
    assertEquals(Map.of("key", "value"), ctx.preferences());
    assertEquals(Set.of(new Authority("ROLE_USER")), ctx.authorities());
  }

  @Test
  void nullUserId_throws() {
    Map<String, Object> prefs = Map.of();
    Set<Authority> auths = Set.of();
    assertThrows(NullPointerException.class, () -> new Context(null, "conv", prefs, auths));
  }

  @Test
  void nullConversationId_throws() {
    Map<String, Object> prefs = Map.of();
    Set<Authority> auths = Set.of();
    assertThrows(NullPointerException.class, () -> new Context("user", null, prefs, auths));
  }

  @Test
  void nullPreferences_defaultsToEmptyMap() {
    var ctx = new Context("user", "conv", null, Set.of());
    assertTrue(ctx.preferences().isEmpty());
  }

  @Test
  void nullAuthorities_defaultsToEmptySet() {
    var ctx = new Context("user", "conv", Map.of(), null);
    assertTrue(ctx.authorities().isEmpty());
  }

  @Test
  void hasAuthority_returnsTrue_whenPresent() {
    var ctx = new Context("user", "conv", Map.of(), Set.of(new Authority("ROLE_ADMIN")));
    assertTrue(ctx.hasAuthority("ROLE_ADMIN"));
  }

  @Test
  void hasAuthority_caseInsensitive() {
    var ctx = new Context("user", "conv", Map.of(), Set.of(new Authority("ROLE_ADMIN")));
    assertTrue(ctx.hasAuthority("role_admin"));
  }

  @Test
  void hasAuthority_returnsFalse_whenAbsent() {
    var ctx = new Context("user", "conv", Map.of(), Set.of(new Authority("ROLE_USER")));
    assertFalse(ctx.hasAuthority("ROLE_ADMIN"));
  }

  @Test
  void anonymous_hasCorrectDefaults() {
    Context ctx = Context.anonymous();
    assertEquals("anonymous", ctx.userId());
    assertEquals("none", ctx.conversationId());
    assertTrue(ctx.preferences().isEmpty());
    assertTrue(ctx.authorities().isEmpty());
  }

  @Test
  void preferences_defensiveCopy() {
    var ctx = new Context("user", "conv", Map.of("key", "value"), Set.of());
    var prefs = ctx.preferences();
    assertThrows(UnsupportedOperationException.class, () -> prefs.put("new", "val"));
  }

  @Test
  void authorities_defensiveCopy() {
    var ctx = new Context("user", "conv", Map.of(), Set.of(new Authority("ROLE_USER")));
    var auths = ctx.authorities();
    var newAuth = new Authority("NEW");
    assertThrows(UnsupportedOperationException.class, () -> auths.add(newAuth));
  }
}
