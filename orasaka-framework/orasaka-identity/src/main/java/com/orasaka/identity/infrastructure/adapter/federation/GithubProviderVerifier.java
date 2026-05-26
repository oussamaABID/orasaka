package com.orasaka.identity.infrastructure.adapter.federation;

import com.fasterxml.jackson.databind.JsonNode;
import com.orasaka.identity.domain.model.ExtractedProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OAuth2 provider verifier for GitHub identity tokens.
 *
 * <p>Conditionally loaded only when {@code spring.security.oauth2.client.registration.github
 * .client-id} is configured. Calls the GitHub User API with the provided access token to extract a
 * canonical {@link ExtractedProfile} for downstream identity reconciliation.
 *
 * <p>Per §2.16 [ERR-120], all HTTP calls use Spring {@link RestClient}.
 */
@Component
@ConditionalOnProperty(
    prefix = "spring.security.oauth2.client.registration.github",
    name = "client-id")
class GithubProviderVerifier extends AbstractProviderVerifier {

  private static final String GITHUB_USER_API = "https://api.github.com/user";

  private final RestClient restClient;

  GithubProviderVerifier() {
    this(RestClient.create());
  }

  GithubProviderVerifier(RestClient restClient) {
    this.restClient = restClient;
    logger.info("GitHub OAuth2 provider verifier initialized");
  }

  @Override
  protected String providerId() {
    return "github";
  }

  @Override
  protected String tokenLabel() {
    return "GitHub access token";
  }

  @Override
  protected ExtractedProfile doVerifyAndExtract(String accessToken) {
    JsonNode userNode =
        restClient
            .get()
            .uri(GITHUB_USER_API)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class);

    if (userNode == null) {
      throw new IllegalArgumentException("GitHub API returned null response for access token");
    }

    String email = extractField(userNode, "email");
    String id = extractField(userNode, "id");
    String name = textOrNull(userNode, "name");
    String login = extractField(userNode, "login");
    String avatarUrl = textOrNull(userNode, "avatar_url");

    // GitHub may return null email if user has private email; fall back to login-based
    if (email == null || email.isBlank()) {
      email = login + "@users.noreply.github.com";
    }

    String displayName = (name != null && !name.isBlank()) ? name : login;

    return new ExtractedProfile(email, id, displayName, avatarUrl);
  }
}
