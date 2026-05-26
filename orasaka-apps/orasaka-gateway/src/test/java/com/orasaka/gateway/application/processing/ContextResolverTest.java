package com.orasaka.gateway.application.processing;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.Context;
import com.orasaka.identity.domain.model.User;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContextResolverTest {

  @Test
  void resolve_delegatesToContextMapper() {
    var user =
        new User(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            "john",
            "john@test.com",
            true,
            Set.of("ROLE_USER"),
            Map.of("language", "en"),
            null,
            "free");
    Context ctx = ContextResolver.resolve(user, "conv-1", null);
    assertNotNull(ctx);
    assertEquals(user.id().toString(), ctx.userId());
    assertEquals("conv-1", ctx.conversationId());
  }
}
