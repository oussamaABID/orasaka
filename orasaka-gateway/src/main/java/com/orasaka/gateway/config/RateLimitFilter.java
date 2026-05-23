package com.orasaka.gateway.config;

import com.orasaka.identity.domain.User;
import com.orasaka.identity.entity.OrasakaRateLimitTierEntity;
import com.orasaka.identity.repository.OrasakaRateLimitTierRepository;
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
 * Servlet filter enforcing dynamic rate limiting based on PostgreSQL tiers and Bucket4j/Lettuce
 * Redis state.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

  private final ProxyManager<String> proxyManager;
  private final OrasakaRateLimitTierRepository tierRepository;
  private final RateLimitProperties properties;
  private final ConcurrentHashMap<String, CachedTier> cache = new ConcurrentHashMap<>();

  /**
   * Constructs a new {@code RateLimitFilter} instance.
   *
   * @param proxyManager The remote Bucket4j proxy manager for atomic remote evaluation.
   * @param tierRepository The Spring Data JPA repository used to retrieve dynamic tiers from
   *     PostgreSQL.
   * @param properties The rate limiting configuration properties mapping from application.yml.
   */
  public RateLimitFilter(
      ProxyManager<String> proxyManager,
      OrasakaRateLimitTierRepository tierRepository,
      RateLimitProperties properties) {
    this.proxyManager = proxyManager;
    this.tierRepository = tierRepository;
    this.properties = properties;
  }

  /**
   * Represents a cached rate limiting tier configuration.
   *
   * @param capacity The maximum capacity (tokens) of the bucket.
   * @param refillTokens The number of tokens refilled in each cycle.
   * @param refillSeconds The refill cycle duration in seconds.
   * @param expiresAt The epoch millisecond timestamp indicating when the cached entry expires.
   */
  record CachedTier(int capacity, int refillTokens, int refillSeconds, long expiresAt) {}

  /**
   * Determines if this filter should not be run on asynchronous dispatches.
   *
   * <p>Overridden to return {@code false} to ensure that streaming client endpoints leveraging
   * Server-Sent Events (SSE) or long-running async threads are rate-limited.
   *
   * @return Always {@code false} to apply the rate limiter on async dispatches.
   */
  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  /**
   * Executes the core rate limiting logic for each HTTP request.
   *
   * <p>Checks standard authentication context for the request. If the user is authenticated, rate
   * limits are evaluated against their user ID. If the request is anonymous, the limit is evaluated
   * against their remote IP. Tiers are fetched dynamically from PostgreSQL and cached in-memory
   * with a 10-second TTL to avoid database overload. Remote buckets are evaluated atomically in
   * Redis using Bucket4j Lettuce.
   *
   * @param request The HTTP request.
   * @param response The HTTP response.
   * @param filterChain The servlet filter chain.
   * @throws ServletException If a servlet exception occurs.
   * @throws IOException If an I/O error occurs.
   */
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

  /**
   * Resolves the rate limit configuration details for the specified tier.
   *
   * <p>Utilizes an in-memory cache with a 10-second TTL to mitigate DB query overhead. If the
   * config is not cached or is expired, it queries PostgreSQL via {@link
   * OrasakaRateLimitTierRepository}.
   *
   * @param tierId The unique identifier of the rate limiting tier (e.g., "free", "premium").
   * @return The resolved {@link CachedTier} config parameters.
   */
  private CachedTier getTierConfig(String tierId) {
    long now = System.currentTimeMillis();
    CachedTier cached = cache.get(tierId);
    if (cached != null && cached.expiresAt() > now) {
      return cached;
    }

    try {
      Optional<OrasakaRateLimitTierEntity> tierOpt = tierRepository.findById(tierId);
      if (tierOpt.isPresent()) {
        OrasakaRateLimitTierEntity tier = tierOpt.get();
        CachedTier resolved =
            new CachedTier(
                tier.getCapacity(),
                tier.getRefillTokens(),
                tier.getRefillSeconds(),
                now + 10000 // 10s TTL
                );
        cache.put(tierId, resolved);
        return resolved;
      }
    } catch (Exception e) {
      logger.warn(
          "Failed to fetch rate limit tier '{}' from database, using fallback config. Cause: {}",
          tierId,
          e.getMessage());
    }

    return new CachedTier(10, 1, 1, now + 10000);
  }
}
