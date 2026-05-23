package com.orasaka.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties record for environment-driven Orasaka rate limiting settings.
 *
 * <p>Maps properties with the {@code orasaka.infrastructure.rate-limit} prefix from {@code
 * application.yml}.
 *
 * @param enabled Whether rate limiting is enabled globally.
 * @param redisUrl The Lettuce-compatible connection URL for Redis.
 * @param defaultTier The fallback tier key if a user context resolves no rate limit tier.
 * @see RateLimitConfig
 * @see RateLimitFilter
 */
@ConfigurationProperties(prefix = "orasaka.infrastructure.rate-limit")
public record RateLimitProperties(boolean enabled, String redisUrl, String defaultTier) {}
