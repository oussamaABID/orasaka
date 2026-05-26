package com.orasaka.gateway.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.ports.outbound.PlatformToolConfigProvider;
import com.orasaka.persistence.domain.model.PlatformToolConfigDto;
import com.orasaka.persistence.domain.ports.inbound.PlatformToolConfigPersistenceProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlatformToolConfigProviderImplTest {

  private final PlatformToolConfigPersistenceProvider persistenceProvider =
      mock(PlatformToolConfigPersistenceProvider.class);
  private final PlatformToolConfigProviderImpl provider =
      new PlatformToolConfigProviderImpl(persistenceProvider);

  @Test
  void getToolConfig_delegatesToPersistence() {
    var dto =
        new PlatformToolConfigDto(1, "tool-1", true, 600, false, "FIXED", "table", Instant.now());
    when(persistenceProvider.findByToolId("tool-1")).thenReturn(Optional.of(dto));
    Optional<PlatformToolConfigProvider.PlatformToolConfig> result =
        provider.getToolConfig("tool-1");
    assertTrue(result.isPresent());
    assertEquals("tool-1", result.get().toolId());
    assertTrue(result.get().cacheEnabled());
    assertEquals(600, result.get().cacheTtlSeconds());
  }

  @Test
  void getToolConfig_notFound_returnsEmpty() {
    when(persistenceProvider.findByToolId("unknown")).thenReturn(Optional.empty());
    assertTrue(provider.getToolConfig("unknown").isEmpty());
  }

  @Test
  void getToolConfig_nullToolId_returnsEmpty() {
    assertTrue(provider.getToolConfig(null).isEmpty());
    verifyNoInteractions(persistenceProvider);
  }

  @Test
  void constructor_nullPersistenceProvider_throws() {
    assertThrows(NullPointerException.class, () -> new PlatformToolConfigProviderImpl(null));
  }
}
