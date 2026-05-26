package com.orasaka.identity.infrastructure.adapter.persistence;

import com.orasaka.identity.domain.model.RateLimitInfo;
import com.orasaka.identity.domain.ports.inbound.RateLimitProvider;
import com.orasaka.persistence.identity.domain.ports.RateLimitPersistenceProvider;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of the RateLimitProvider inbound port. Follows ERR-105. */
@Service
@Transactional(readOnly = true)
class RateLimitProviderImpl implements RateLimitProvider {

  private final RateLimitPersistenceProvider provider;

  RateLimitProviderImpl(RateLimitPersistenceProvider provider) {
    this.provider = Objects.requireNonNull(provider, "RateLimitPersistenceProvider cannot be null");
  }

  @Override
  public Optional<RateLimitInfo> getRateLimit(String tierKey) {
    Objects.requireNonNull(tierKey, "Tier key cannot be null");
    return provider.findById(tierKey).map(RateLimitMapper::toInfo);
  }
}
