package com.orasaka.gateway.config;

import com.orasaka.identity.domain.User;
import com.orasaka.identity.entity.RateLimitTierEntity;
import com.orasaka.identity.repository.RateLimitTierRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * HTTP request rate-limiting filter using Bucket4j token-bucket algorithm.
 *
 * <p>Applies per-user (authenticated) or per-IP (anonymous) rate limiting based on configurable
 * tiers stored in the database. Tier configurations are cached in-memory for 10 seconds to minimize
 * database roundtrips.
 *
 * <p>Responds with HTTP 429 and JSON error body when the rate limit is exceeded. Sets {@code
 * X-RateLimit-Limit} and {@code X-RateLimit-Remaining} headers on all responses.
 *
 * @see RateLimitProperties
 * @see RateLimitConfig
 * @since 1.0.0
 */
class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

  private final ProxyManager<String> proxyManager;
  private final RateLimitTierRepository tierRepository;
  private final RateLimitProperties properties;
  private final ConcurrentHashMap<String, CachedTier> cache = new ConcurrentHashMap<>();

  /**
   * Constructs the rate limit filter.
   *
   * @param proxyManager Bucket4j proxy manager for distributed buckets.
   * @param tierRepository Repository for resolving rate limit tier configurations.
   * @param properties Rate limit default configuration properties.
   */
  public RateLimitFilter(
      ProxyManager<String> proxyManager,
      RateLimitTierRepository tierRepository,
      RateLimitProperties properties) {
    this.proxyManager = proxyManager;
    this.tierRepository = tierRepository;
    this.properties = properties;
  }

  /**
   * Cached rate limit tier configuration with TTL expiry.
   *
   * @param capacity Maximum bucket capacity (tokens).
   * @param refillTokens Number of tokens refilled per interval.
   * @param refillSeconds Refill interval duration in seconds.
   * @param expiresAt Cache entry expiry timestamp (millis since epoch).
   */
  record CachedTier(int capacity, int refillTokens, int refillSeconds, long expiresAt) {}

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Optional<User> authenticatedUser =
        Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(auth -> auth.getPrincipal() instanceof User)
            .map(auth -> (User) auth.getPrincipal());

    String key =
        authenticatedUser.map(user -> user.id().toString()).orElseGet(request::getRemoteAddr);

    String resolvedTier =
        authenticatedUser
            .map(User::rateLimitTier)
            .filter(tier -> !tier.isBlank())
            .orElseGet(properties::defaultTier);

    CachedTier tierConfig = getTierConfig(resolvedTier);

    BucketConfiguration configuration =
        BucketConfiguration.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(tierConfig.capacity())
                    .refillIntervally(
                        tierConfig.refillTokens(), Duration.ofSeconds(tierConfig.refillSeconds()))
                    .build())
            .build();

    String bucketKey = key + ":" + resolvedTier;
    Bucket bucket = proxyManager.builder().build(bucketKey, () -> configuration);

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.setHeader("X-RateLimit-Limit", String.valueOf(tierConfig.capacity()));
      response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(429);
      response.setHeader("X-RateLimit-Limit", String.valueOf(tierConfig.capacity()));
      response.setHeader("X-RateLimit-Remaining", "0");
      response.setHeader("Content-Type", "application/json");
      response.getWriter().write("{\"error\": \"Too Many Requests\"}");
    }
  }

  private CachedTier getTierConfig(String tierId) {
    long now = System.currentTimeMillis();
    CachedTier cached = cache.get(tierId);
    if (cached != null && cached.expiresAt() > now) {
      return cached;
    }

    try {
      Optional<RateLimitTierEntity> tierOpt = tierRepository.findById(tierId);
      if (tierOpt.isPresent()) {
        RateLimitTierEntity tier = tierOpt.get();
        CachedTier resolved =
            new CachedTier(
                tier.getCapacity(), tier.getRefillTokens(), tier.getRefillSeconds(), now + 10000);
        cache.put(tierId, resolved);
        return resolved;
      }
    } catch (Exception e) {
      logger.error(
          "Failed to fetch rate limit tier '{}' from database, using fallback config.", tierId, e);
    }

    return new CachedTier(10, 1, 1, now + 10000);
  }
}
