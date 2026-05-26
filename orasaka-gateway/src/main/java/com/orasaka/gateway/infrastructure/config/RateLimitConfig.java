package com.orasaka.gateway.infrastructure.config;

import com.orasaka.identity.domain.ports.inbound.RateLimitProvider;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
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
public class RateLimitConfig {

  private final RateLimitProperties properties;
  private final String redisUrl;

  /**
   * Constructs the rate limiting configuration class with injected environment properties.
   *
   * @param properties The configuration properties mapping {@code
   *     orasaka.infrastructure.rate-limit}.
   * @param redisUrl The Spring Boot Redis configuration URL.
   */
  public RateLimitConfig(
      RateLimitProperties properties, @Value("${spring.data.redis.url}") String redisUrl) {
    this.properties = properties;
    this.redisUrl = redisUrl;
  }

  /**
   * Instantiates the standalone Lettuce {@link RedisClient} bean.
   *
   * @return The configured and ready {@link RedisClient} instance.
   * @see io.lettuce.core.RedisClient
   */
  @Bean(destroyMethod = "shutdown")
  public RedisClient redisClient() {
    return RedisClient.create(RedisURI.create(redisUrl));
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
  @ConditionalOnProperty(name = "orasaka.infrastructure.rate-limit.enabled", havingValue = "true")
  public ProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> connection) {
    return Bucket4jLettuce.casBasedBuilder(connection).build();
  }

  /**
   * Instantiates the custom {@link RateLimitFilter} bean using the Lettuce proxy manager and a JDBC
   * template to retrieve database tiers.
   *
   * @param proxyManager The remote bucket manager.
   * @param rateLimitProvider The provider to retrieve database rate limit tiers.
   * @return The rate limiting servlet filter instance.
   * @see RateLimitFilter
   */
  @Bean
  @ConditionalOnProperty(name = "orasaka.infrastructure.rate-limit.enabled", havingValue = "true")
  public RateLimitFilter rateLimitFilter(
      ProxyManager<String> proxyManager, RateLimitProvider rateLimitProvider) {
    return new RateLimitFilter(proxyManager, rateLimitProvider, properties);
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
  @ConditionalOnProperty(name = "orasaka.infrastructure.rate-limit.enabled", havingValue = "true")
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
      RateLimitFilter filter) {
    FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
