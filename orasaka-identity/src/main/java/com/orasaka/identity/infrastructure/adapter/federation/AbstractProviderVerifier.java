package com.orasaka.identity.infrastructure.adapter.federation;

import com.orasaka.identity.domain.model.ExtractedProfile;
import com.orasaka.identity.domain.ports.outbound.OAuth2ProviderVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for OAuth2 provider verifiers.
 *
 * <p>Encapsulates the common token-validation contract shared by all provider implementations
 * (null-check, logging, delegation to the provider-specific verification method).
 *
 * @see OAuth2ProviderVerifier
 */
abstract class AbstractProviderVerifier implements OAuth2ProviderVerifier {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /** Returns the lowercase canonical provider identifier (e.g. "github", "google"). */
  protected abstract String providerId();

  /** Returns a human-readable label for log messages (e.g. "GitHub access token"). */
  protected abstract String tokenLabel();

  /**
   * Provider-specific verification logic.
   *
   * @param idToken the non-null, non-blank token to verify.
   * @return the extracted canonical profile.
   */
  protected abstract ExtractedProfile doVerifyAndExtract(String idToken);

  @Override
  public boolean supports(String providerId) {
    return providerId().equalsIgnoreCase(providerId);
  }

  @Override
  public ExtractedProfile verifyAndExtract(String idToken) {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalArgumentException(tokenLabel() + " cannot be null or empty");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Verifying {} (length={})", tokenLabel(), idToken.length());
    }
    return doVerifyAndExtract(idToken);
  }
}
