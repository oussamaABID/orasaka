package com.orasaka.gateway.application.service;

import com.orasaka.core.domain.ports.outbound.UserMcpServerProvider;
import com.orasaka.persistence.identity.domain.ports.UserMcpServerPersistenceProvider;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Implementation of the outbound UserMcpServerProvider port using the identity persistence
 * provider.
 */
@Service
class UserMcpServerProviderImpl implements UserMcpServerProvider {

  private final UserMcpServerPersistenceProvider persistenceProvider;

  UserMcpServerProviderImpl(UserMcpServerPersistenceProvider persistenceProvider) {
    this.persistenceProvider =
        Objects.requireNonNull(persistenceProvider, "persistenceProvider must not be null");
  }

  @Override
  public List<UserMcpServer> getActiveUserMcpServers(String userId) {
    if (userId == null) {
      return List.of();
    }
    return persistenceProvider.findByUserIdAndEnabledTrue(userId).stream()
        .map(
            dto ->
                new UserMcpServer(
                    dto.id(), dto.userId(), dto.label(), dto.url(), dto.authToken(), dto.enabled()))
        .toList();
  }
}
