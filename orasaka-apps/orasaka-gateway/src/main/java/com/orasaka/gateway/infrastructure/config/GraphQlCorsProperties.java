package com.orasaka.gateway.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties record for native Spring GraphQL CORS settings.
 *
 * <p>Maps properties under the {@code spring.graphql.cors} prefix in {@code application.yml}
 * automatically.
 */
@ConfigurationProperties(prefix = "spring.graphql.cors")
public record GraphQlCorsProperties(
    List<String> allowedOrigins,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    Boolean allowCredentials) {}
