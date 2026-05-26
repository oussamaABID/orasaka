package com.orasaka.gateway.application.service;

import com.orasaka.core.domain.ports.outbound.PlatformMcpServerProvider;
import com.orasaka.persistence.domain.ports.inbound.PlatformMcpServerPersistenceProvider;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Implementation of the outbound PlatformMcpServerProvider port using the app persistence provider.
 */
@Service
class PlatformMcpServerProviderImpl implements PlatformMcpServerProvider {

  private final PlatformMcpServerPersistenceProvider persistenceProvider;

  PlatformMcpServerProviderImpl(PlatformMcpServerPersistenceProvider persistenceProvider) {
    this.persistenceProvider =
        Objects.requireNonNull(persistenceProvider, "persistenceProvider must not be null");
  }

  @Override
  public List<PlatformMcpServer> getActivePlatformMcpServers() {
    return persistenceProvider.findByEnabledTrue().stream()
        .map(
            dto ->
                new PlatformMcpServer(
                    dto.id(),
                    dto.label(),
                    dto.transportType(),
                    dto.url(),
                    dto.command(),
                    dto.args(),
                    dto.authToken(),
                    dto.enabled()))
        .toList();
  }
}
