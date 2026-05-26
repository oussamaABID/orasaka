package com.orasaka.gateway.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.ports.outbound.UserMcpServerProvider;
import com.orasaka.persistence.identity.domain.model.UserMcpServerDto;
import com.orasaka.persistence.identity.domain.ports.UserMcpServerPersistenceProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserMcpServerProviderImplTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  private final UserMcpServerPersistenceProvider persistenceProvider =
      mock(UserMcpServerPersistenceProvider.class);
  private final UserMcpServerProviderImpl provider =
      new UserMcpServerProviderImpl(persistenceProvider);

  @Test
  void getActiveUserMcpServers_mapsFromPersistence() {
    var dto =
        new UserMcpServerDto(
            1, "user-1", "My MCP", "http://localhost", "token", true, Instant.now(FIXED_CLOCK));
    when(persistenceProvider.findByUserIdAndEnabledTrue("user-1")).thenReturn(List.of(dto));
    List<UserMcpServerProvider.UserMcpServer> result = provider.getActiveUserMcpServers("user-1");
    assertEquals(1, result.size());
    assertEquals("My MCP", result.get(0).label());
    assertEquals("http://localhost", result.get(0).url());
  }

  @Test
  void getActiveUserMcpServers_nullUserId_returnsEmpty() {
    List<UserMcpServerProvider.UserMcpServer> result = provider.getActiveUserMcpServers(null);
    assertTrue(result.isEmpty());
    verifyNoInteractions(persistenceProvider);
  }

  @Test
  void constructor_nullPersistenceProvider_throws() {
    assertThrows(NullPointerException.class, () -> new UserMcpServerProviderImpl(null));
  }
}
