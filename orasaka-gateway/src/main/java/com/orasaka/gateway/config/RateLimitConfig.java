package com.orasaka.gateway.config;

import com.orasaka.identity.repository.OrasakaRateLimitTierRepository;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditional configuration class for the dynamic rate limiting engine.
 *
 * <p>Only instantiates client connection pools to Redis when the master toggle {@code
 * orasaka.infrastructure.rate-limit.enabled} is set to {@code true}.
 */
@Configuration
@ConditionalOnProperty(name = "orasaka.infrastructure.rate-limit.enabled", havingValue = "true")
public class RateLimitConfig {

  private final RateLimitProperties properties;

  /**
   * Constructs the rate limiting configuration class with injected environment properties.
   *
   * @param properties The configuration properties mapping {@code
   *     orasaka.infrastructure.rate-limit}.
   */
  public RateLimitConfig(RateLimitProperties properties) {
    this.properties = properties;
  }

  /**
   * Instantiates the standalone Lettuce {@link RedisClient} bean.
   *
   * @return The configured and ready {@link RedisClient} instance.
   * @see io.lettuce.core.RedisClient
   */
  @Bean(destroyMethod = "shutdown")
  public RedisClient redisClient() {
    return RedisClient.create(RedisURI.create(properties.redisUrl()));
  }

  /**
   * Opens a stateful connection to the Redis server using a UTF-8 String codec for keys and a raw
   * byte array codec for values.
   *
   * @param redisClient The active Lettuce Redis client.
   * @return A thread-safe, stateful connection bean to Redis.
   * @see io.lettuce.core.api.StatefulRedisConnection
   */
  @Bean(destroyMethod = "close")
  public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
    return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
  }

  /**
   * Builds the Bucket4j Lettuce-based {@link ProxyManager} to allow atomic remote bucket
   * evaluation.
   *
   * @param connection The stateful connection to Redis.
   * @return The Lettuce-backed proxy manager bean.
   * @see io.github.bucket4j.distributed.proxy.ProxyManager
   */
  @Bean
  @SuppressWarnings("deprecation")
  public ProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> connection) {
    return LettuceBasedProxyManager.builderFor(connection).build();
  }

  /**
   * Instantiates the custom {@link RateLimitFilter} bean using the Lettuce proxy manager and a JDBC
   * template to retrieve database tiers.
   *
   * @param proxyManager The remote bucket manager.
   * @param tierRepository The repository to retrieve database rate limit tiers.
   * @return The rate limiting servlet filter instance.
   * @see RateLimitFilter
   */
  @Bean
  public RateLimitFilter rateLimitFilter(
      ProxyManager<String> proxyManager, OrasakaRateLimitTierRepository tierRepository) {
    return new RateLimitFilter(proxyManager, tierRepository, properties);
  }

  /**
   * Configures a {@link FilterRegistrationBean} to disable automatic container registration of the
   * filter.
   *
   * <p>This prevents the filter from intercepting requests prior to standard Spring Security,
   * allowing user authentication context to be resolved first.
   *
   * @param filter The custom rate limit filter bean.
   * @return A disabled filter registration bean.
   * @see SecurityConfig
   */
  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
      RateLimitFilter filter) {
    FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
