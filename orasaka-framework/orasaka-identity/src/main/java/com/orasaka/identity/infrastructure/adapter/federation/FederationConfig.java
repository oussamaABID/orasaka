package com.orasaka.identity.infrastructure.adapter.federation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Package-private configuration class declaring OAuth2 provider verifiers.
 *
 * <p>By declaring these beans programmatically instead of marking classes with {@code @Component},
 * we avoid constructor autowiring issues and remain fully aligned with Spring configuration best
 * practices.
 */
@Configuration
class FederationConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "spring.security.oauth2.client.registration.google",
      name = "client-id")
  GoogleProviderVerifier googleProviderVerifier(
      RestClient identityRestClient, ObjectMapper objectMapper) {
    return new GoogleProviderVerifier(identityRestClient, objectMapper);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "spring.security.oauth2.client.registration.github",
      name = "client-id")
  GithubProviderVerifier githubProviderVerifier(RestClient identityRestClient) {
    return new GithubProviderVerifier(identityRestClient);
  }
}
