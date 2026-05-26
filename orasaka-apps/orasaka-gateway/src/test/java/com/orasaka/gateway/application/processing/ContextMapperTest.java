package com.orasaka.gateway.application.processing;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.Context;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserProfile;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContextMapperTest {

  private static User testUser() {
    return new User(
        UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
        "john",
        "john@test.com",
        true,
        Set.of("ROLE_USER"),
        Map.of("language", "en"),
        null,
        "free");
  }

  @Test
  void buildContext_withNullProfileProvider_setsBasicFields() {
    var user = testUser();
    Context ctx = ContextMapper.buildContext(user, "conv-1", UUID.randomUUID(), null);
    assertEquals(user.id().toString(), ctx.userId());
    assertEquals("conv-1", ctx.conversationId());
    assertEquals("en", ctx.preferences().get("language"));
    assertFalse(ctx.authorities().isEmpty());
  }

  @Test
  @SuppressWarnings("unchecked")
  void buildContext_withProfileProvider_mergesPreferences() {
    var user = testUser();
    UserProfileProvider provider =
        userId ->
            new UserProfile(
                userId, "dark", "shimmer", "finance", "creative", Map.of("extra", "value"));
    UserProfile profile = provider.getProfile(user.id().toString());
    Context ctx = ContextMapper.buildContext(user, "conv-2", UUID.randomUUID(), profile);
    
    Map<String, Object> userProfileContext = (Map<String, Object>) ctx.preferences().get("userProfileContext");
    assertNotNull(userProfileContext);
    assertEquals("dark", userProfileContext.get("theme"));
    assertEquals("shimmer", userProfileContext.get("voiceModel"));
    assertEquals("finance", userProfileContext.get("primaryIndustry"));
    assertEquals("creative", userProfileContext.get("aiBehavior"));
    assertEquals("value", ctx.preferences().get("extra"));
  }

  @Test
  void buildContext_withProviderReturningNull_fallsBackToUserPreferences() {
    var user = testUser();
    UserProfileProvider provider = userId -> null;
    UserProfile profile = provider.getProfile(user.id().toString());
    Context ctx = ContextMapper.buildContext(user, "conv-1", UUID.randomUUID(), profile);
    assertEquals("en", ctx.preferences().get("language"));
    assertNull(ctx.preferences().get("theme"));
  }
}
