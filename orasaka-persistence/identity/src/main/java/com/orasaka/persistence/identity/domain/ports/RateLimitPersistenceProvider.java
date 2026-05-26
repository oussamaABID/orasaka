package com.orasaka.persistence.identity.domain.ports;

import com.orasaka.persistence.identity.domain.model.RateLimitDto;
import java.util.Optional;

/** Port interface for managing RateLimit persistence operations. */
public interface RateLimitPersistenceProvider {

  Optional<RateLimitDto> findById(String tierKey);

  RateLimitDto save(RateLimitDto rateLimitDto);
}
