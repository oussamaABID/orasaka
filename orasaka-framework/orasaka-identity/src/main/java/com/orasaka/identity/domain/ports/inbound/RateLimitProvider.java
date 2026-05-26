package com.orasaka.identity.domain.ports.inbound;

import com.orasaka.identity.domain.model.RateLimitInfo;
import java.util.Optional;

/** Inbound port contract for resolving rate limit configs inside the identity domain. */
public interface RateLimitProvider {

  /**
   * Resolves the rate limit configuration details for a given tier key.
   *
   * @param tierKey The target tier ID.
   * @return Optional containing RateLimitInfo if registered.
   */
  Optional<RateLimitInfo> getRateLimit(String tierKey);
}
