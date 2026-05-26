package com.orasaka.identity.infrastructure.adapter.federation;

import com.orasaka.identity.domain.model.ExtractedProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OAuth2 provider verifier for Google identity tokens.
 *
 * <p>Conditionally loaded only when {@code spring.security.oauth2.client.registration.google
 * .client-id} is configured. Verifies Google-issued ID tokens and extracts a canonical {@link
 * ExtractedProfile} for downstream identity reconciliation.
 *
 * <p><strong>Extension point:</strong> Override {@link #doVerifyAndExtract(String)} to decode the
 * JWT and verify its signature against Google's JWKS endpoint ({@code
 * https://www.googleapis.com/oauth2/v3/certs}).
 */
@Component
@ConditionalOnProperty(
    prefix = "spring.security.oauth2.client.registration.google",
    name = "client-id")
class GoogleProviderVerifier extends AbstractProviderVerifier {

  GoogleProviderVerifier() {
    logger.info("Google OAuth2 provider verifier initialized");
  }

  @Override
  protected String providerId() {
    return "google";
  }

  @Override
  protected String tokenLabel() {
    return "Google ID token";
  }

  @Override
  protected ExtractedProfile doVerifyAndExtract(String idToken) {
    // Extension point: Implement JWT decoding and Google JWKS signature verification.
    // Decode JWT, verify signature via Google JWKS, extract claims.
    throw new UnsupportedOperationException(
        "Google token verification requires JWT decoding implementation. "
            + "Connect to https://www.googleapis.com/oauth2/v3/certs for JWKS validation.");
  }
}
