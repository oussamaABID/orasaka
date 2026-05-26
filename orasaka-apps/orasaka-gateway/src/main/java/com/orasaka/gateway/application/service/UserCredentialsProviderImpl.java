package com.orasaka.gateway.application.service;

import com.orasaka.core.domain.ports.outbound.UserCredentialsProvider;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Implementation of UserCredentialsProvider port to resolve decrypted API key credentials for core.
 */
@Service
class UserCredentialsProviderImpl implements UserCredentialsProvider {

  private final IdentityService identityService;

  UserCredentialsProviderImpl(IdentityService identityService) {
    this.identityService =
        Objects.requireNonNull(identityService, "IdentityService must not be null");
  }

  @Override
  public Optional<String> getDecryptedApiKey(String userId, String providerName) {
    if (userId == null || providerName == null) {
      return Optional.empty();
    }
    return identityService.getDecryptedApiKey(userId, providerName);
  }
}
