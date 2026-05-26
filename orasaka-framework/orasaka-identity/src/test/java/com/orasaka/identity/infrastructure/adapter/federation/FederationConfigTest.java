package com.orasaka.identity.infrastructure.adapter.federation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Unit tests for {@link FederationConfig}. */
class FederationConfigTest {

  private final FederationConfig config = new FederationConfig();

  @Test
  void shouldCreateGoogleProviderVerifier() {
    RestClient restClient = mock(RestClient.class);
    ObjectMapper objectMapper = mock(ObjectMapper.class);
    GoogleProviderVerifier verifier = config.googleProviderVerifier(restClient, objectMapper);
    assertNotNull(verifier);
  }

  @Test
  void shouldCreateGithubProviderVerifier() {
    RestClient restClient = mock(RestClient.class);
    GithubProviderVerifier verifier = config.githubProviderVerifier(restClient);
    assertNotNull(verifier);
  }
}
