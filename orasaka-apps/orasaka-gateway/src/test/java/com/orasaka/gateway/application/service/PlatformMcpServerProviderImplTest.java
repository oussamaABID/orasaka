package com.orasaka.gateway.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.ports.outbound.PlatformMcpServerProvider;
import com.orasaka.persistence.domain.model.PlatformMcpServerDto;
import com.orasaka.persistence.domain.ports.inbound.PlatformMcpServerPersistenceProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformMcpServerProviderImplTest {

  private final PlatformMcpServerPersistenceProvider persistenceProvider =
      mock(PlatformMcpServerPersistenceProvider.class);
  private final PlatformMcpServerProviderImpl provider =
      new PlatformMcpServerProviderImpl(persistenceProvider);

  @Test
  void getActivePlatformMcpServers_mapsFromPersistence() {
    var dto =
        new PlatformMcpServerDto(
            1, "GitHub", "STDIO", "url", "node", "--args", "token", true, Instant.now());
    when(persistenceProvider.findByEnabledTrue()).thenReturn(List.of(dto));
    List<PlatformMcpServerProvider.PlatformMcpServer> result =
        provider.getActivePlatformMcpServers();
    assertEquals(1, result.size());
    assertEquals("GitHub", result.get(0).label());
    assertEquals("STDIO", result.get(0).transportType());
  }

  @Test
  void getActivePlatformMcpServers_emptyList() {
    when(persistenceProvider.findByEnabledTrue()).thenReturn(List.of());
    assertTrue(provider.getActivePlatformMcpServers().isEmpty());
  }

  @Test
  void constructor_nullPersistenceProvider_throws() {
    assertThrows(NullPointerException.class, () -> new PlatformMcpServerProviderImpl(null));
  }
}
