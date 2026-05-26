package com.orasaka.identity.infrastructure.adapter.federation;

import com.orasaka.identity.domain.model.ExtractedProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OAuth2 provider verifier for GitHub identity tokens.
 *
 * <p>Conditionally loaded only when {@code spring.security.oauth2.client.registration.github
 * .client-id} is configured. Verifies GitHub-issued access tokens and extracts a canonical {@link
 * ExtractedProfile} for downstream identity reconciliation.
 *
 * <p><strong>Extension point:</strong> Override {@link #doVerifyAndExtract(String)} to call the
 * GitHub User API ({@code https://api.github.com/user}) with the provided access token.
 */
@Component
@ConditionalOnProperty(
    prefix = "spring.security.oauth2.client.registration.github",
    name = "client-id")
class GithubProviderVerifier extends AbstractProviderVerifier {

  GithubProviderVerifier() {
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
  protected ExtractedProfile doVerifyAndExtract(String idToken) {
    // Extension point: Implement GitHub API call to verify token and extract profile.
    // Call GET https://api.github.com/user with Bearer token header.
    // Parse JSON response for login, email, avatar_url, and id fields.
    throw new UnsupportedOperationException(
        "GitHub token verification requires API call implementation. "
            + "Call https://api.github.com/user with Bearer token for profile extraction.");
  }
}
