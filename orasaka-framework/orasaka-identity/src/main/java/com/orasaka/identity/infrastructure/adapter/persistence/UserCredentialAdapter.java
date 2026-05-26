package com.orasaka.identity.infrastructure.adapter.persistence;

import com.orasaka.identity.domain.model.UserCredential;
import com.orasaka.identity.domain.ports.outbound.UserCredentialRepositoryPort;
import com.orasaka.persistence.identity.domain.model.UserCredentialDto;
import com.orasaka.persistence.identity.domain.ports.UserCredentialPersistenceProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Concrete persistence adapter implementing {@link UserCredentialRepositoryPort}. Delegates
 * persistence requests to the clean UserCredentialPersistenceProvider port.
 */
@Component
class UserCredentialAdapter implements UserCredentialRepositoryPort {

  private static final String USER_ID_NULL = "UserId cannot be null";
  private static final String PROVIDER_NAME_NULL = "ProviderName cannot be null";
  private final UserCredentialPersistenceProvider provider;

  UserCredentialAdapter(UserCredentialPersistenceProvider provider) {
    this.provider =
        Objects.requireNonNull(provider, "UserCredentialPersistenceProvider cannot be null");
  }

  @Override
  public List<UserCredential> findByUserId(String userId) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    return provider.findByUserId(userId).stream()
        .map(dto -> new UserCredential(dto.providerName(), true))
        .toList();
  }

  @Override
  public Optional<String> findApiKeyByUserIdAndProviderName(String userId, String providerName) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    return provider
        .findByUserIdAndProviderName(userId, providerName)
        .map(UserCredentialDto::apiKey);
  }

  @Override
  public void save(String userId, String providerName, String apiKey) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    Objects.requireNonNull(apiKey, "ApiKey cannot be null");
    UserCredentialDto dto = new UserCredentialDto(null, userId, providerName, apiKey);
    provider.save(dto);
  }

  @Override
  public void deleteByUserIdAndProviderName(String userId, String providerName) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    provider.deleteByUserIdAndProviderName(userId, providerName);
  }
}
