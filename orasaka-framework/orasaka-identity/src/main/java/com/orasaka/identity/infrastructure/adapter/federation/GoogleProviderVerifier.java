package com.orasaka.identity.infrastructure.adapter.federation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.identity.domain.model.ExtractedProfile;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OAuth2 provider verifier for Google identity tokens.
 *
 * <p>Conditionally loaded only when {@code spring.security.oauth2.client.registration.google
 * .client-id} is configured. Decodes the Google-issued JWT ID token, validates the issuer and
 * expiration claims, and extracts a canonical {@link ExtractedProfile}.
 *
 * <p>Verification flow:
 *
 * <ol>
 *   <li>Decode the JWT payload (Base64URL middle segment).
 *   <li>Validate issuer is {@code accounts.google.com} or {@code https://accounts.google.com}.
 *   <li>Validate the token is not expired ({@code exp} claim).
 *   <li>Extract {@code sub}, {@code email}, {@code name}, {@code picture} claims.
 * </ol>
 *
 * <p>For production-grade JWKS signature verification, the {@code tokeninfo} endpoint is called as
 * a secondary validation step.
 *
 * <p>Per §2.16 [ERR-120], all HTTP calls use Spring {@link RestClient}.
 */
@Component
@ConditionalOnProperty(
    prefix = "spring.security.oauth2.client.registration.google",
    name = "client-id")
class GoogleProviderVerifier extends AbstractProviderVerifier {

  private static final String GOOGLE_TOKENINFO_URL =
      "https://oauth2.googleapis.com/tokeninfo?id_token=";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  GoogleProviderVerifier() {
    this(RestClient.create(), new ObjectMapper());
  }

  GoogleProviderVerifier(RestClient restClient, ObjectMapper objectMapper) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
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
    // Step 1: Decode JWT payload for claim extraction
    JsonNode payload = decodeJwtPayload(idToken);

    // Step 2: Validate issuer
    String issuer = textOrNull(payload, "iss");
    if (!"accounts.google.com".equals(issuer) && !"https://accounts.google.com".equals(issuer)) {
      throw new IllegalArgumentException("Invalid Google token issuer: " + issuer);
    }

    // Step 3: Validate expiration
    long exp = payload.has("exp") ? payload.get("exp").asLong() : 0;
    long now = System.currentTimeMillis() / 1000;
    if (exp < now) {
      throw new IllegalArgumentException(
          "Google ID token has expired (exp=" + exp + ", now=" + now + ")");
    }

    // Step 4: Server-side validation via Google tokeninfo endpoint
    JsonNode tokenInfo = verifyViaTokenInfo(idToken);

    // Step 5: Extract profile from verified token
    String sub = extractRequired(tokenInfo, "sub", "Google user ID");
    String email = extractRequired(tokenInfo, "email", "email");
    String name = textOrNull(tokenInfo, "name");
    String picture = textOrNull(tokenInfo, "picture");

    return new ExtractedProfile(email, sub, name, picture);
  }

  private JsonNode decodeJwtPayload(String jwt) {
    String[] parts = jwt.split("\\.");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid JWT format: expected at least 2 segments");
    }
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
      return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to decode Google JWT payload", e);
    }
  }

  private JsonNode verifyViaTokenInfo(String idToken) {
    JsonNode tokenInfo =
        restClient
            .get()
            .uri(GOOGLE_TOKENINFO_URL + idToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class);

    if (tokenInfo == null) {
      throw new IllegalArgumentException("Google tokeninfo API returned null response");
    }

    if (tokenInfo.has("error_description")) {
      throw new IllegalArgumentException(
          "Google token verification failed: " + tokenInfo.get("error_description").asText());
    }

    return tokenInfo;
  }

  private String extractRequired(JsonNode node, String field, String label) {
    String value = textOrNull(node, field);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Google token is missing required field: " + label);
    }
    return value;
  }
}
