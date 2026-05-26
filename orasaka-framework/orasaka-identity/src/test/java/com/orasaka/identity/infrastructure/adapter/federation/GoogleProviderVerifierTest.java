package com.orasaka.identity.infrastructure.adapter.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orasaka.identity.domain.model.ExtractedProfile;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class GoogleProviderVerifierTest {

  private RestClient restClient;
  private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
  private RestClient.RequestHeadersSpec<?> requestHeadersSpec;
  private RestClient.ResponseSpec responseSpec;
  private GoogleProviderVerifier verifier;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    restClient = mock(RestClient.class);
    requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    responseSpec = mock(RestClient.ResponseSpec.class);

    doReturn(requestHeadersUriSpec).when(restClient).get();
    doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(String.class));
    doReturn(requestHeadersSpec).when(requestHeadersSpec).accept(any(MediaType.class));
    doReturn(responseSpec).when(requestHeadersSpec).retrieve();

    verifier = new GoogleProviderVerifier(restClient, objectMapper);
  }

  @Test
  void supports_google_returnsTrue() {
    assertThat(verifier.supports("google")).isTrue();
    assertThat(verifier.supports("GOOGLE")).isTrue();
  }

  @Test
  void supports_other_returnsFalse() {
    assertThat(verifier.supports("github")).isFalse();
  }

  @Test
  void tokenLabel_returnsGoogleIdToken() {
    assertThat(verifier.tokenLabel()).isEqualTo("Google ID token");
  }

  @Test
  void doVerifyAndExtract_validToken_returnsExtractedProfile() {
    String jwt = createJwt("accounts.google.com", futureEpoch());
    ObjectNode tokenInfo = createTokenInfoResponse();

    when(responseSpec.body(JsonNode.class)).thenReturn(tokenInfo);

    ExtractedProfile profile = verifier.doVerifyAndExtract(jwt);

    assertThat(profile.email()).isEqualTo("user@gmail.com");
    assertThat(profile.providerId()).isEqualTo("google-sub-123");
    assertThat(profile.name()).isEqualTo("Test User");
    assertThat(profile.avatarUrl()).isEqualTo("https://lh3.googleusercontent.com/photo");
  }

  @Test
  void doVerifyAndExtract_httpsIssuer_succeeds() {
    String jwt = createJwt("https://accounts.google.com", futureEpoch());
    ObjectNode tokenInfo = createTokenInfoResponse();

    when(responseSpec.body(JsonNode.class)).thenReturn(tokenInfo);

    ExtractedProfile profile = verifier.doVerifyAndExtract(jwt);

    assertThat(profile.email()).isEqualTo("user@gmail.com");
  }

  @Test
  void doVerifyAndExtract_invalidIssuer_throwsIllegalArgument() {
    String jwt = createJwt("evil.com", futureEpoch());

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid Google token issuer");
  }

  @Test
  void doVerifyAndExtract_expiredToken_throwsIllegalArgument() {
    String jwt = createJwt("accounts.google.com", (System.currentTimeMillis() / 1000) - 3600);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google ID token has expired");
  }

  @Test
  void doVerifyAndExtract_invalidJwtFormat_throwsIllegalArgument() {
    assertThatThrownBy(() -> verifier.doVerifyAndExtract("not-a-jwt"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid JWT format");
  }

  @Test
  void doVerifyAndExtract_corruptedPayload_throwsIllegalArgument() {
    assertThatThrownBy(() -> verifier.doVerifyAndExtract("header.!!!invalid!!!.signature"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Failed to decode Google JWT payload");
  }

  @Test
  void doVerifyAndExtract_tokenInfoNull_throwsIllegalArgument() {
    String jwt = createJwt("accounts.google.com", futureEpoch());

    when(responseSpec.body(JsonNode.class)).thenReturn(null);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google tokeninfo API returned null response");
  }

  @Test
  void doVerifyAndExtract_tokenInfoError_throwsIllegalArgument() {
    String jwt = createJwt("accounts.google.com", futureEpoch());

    ObjectNode errorResponse = objectMapper.createObjectNode();
    errorResponse.put("error_description", "Invalid token");

    when(responseSpec.body(JsonNode.class)).thenReturn(errorResponse);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google token verification failed: Invalid token");
  }

  @Test
  void doVerifyAndExtract_missingSub_throwsIllegalArgument() {
    String jwt = createJwt("accounts.google.com", futureEpoch());

    ObjectNode tokenInfo = objectMapper.createObjectNode();
    tokenInfo.put("email", "user@gmail.com");

    when(responseSpec.body(JsonNode.class)).thenReturn(tokenInfo);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing required field: Google user ID");
  }

  @Test
  void doVerifyAndExtract_missingEmail_throwsIllegalArgument() {
    String jwt = createJwt("accounts.google.com", futureEpoch());

    ObjectNode tokenInfo = objectMapper.createObjectNode();
    tokenInfo.put("sub", "google-sub-123");

    when(responseSpec.body(JsonNode.class)).thenReturn(tokenInfo);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing required field: email");
  }

  @Test
  void verifyAndExtract_nullToken_throwsIllegalArgument() {
    assertThatThrownBy(() -> verifier.verifyAndExtract(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google ID token cannot be null or empty");
  }

  @Test
  void verifyAndExtract_blankToken_throwsIllegalArgument() {
    assertThatThrownBy(() -> verifier.verifyAndExtract("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google ID token cannot be null or empty");
  }

  // --- Helpers ---

  private String createJwt(String issuer, long exp) {
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                ("{\"iss\":\""
                        + issuer
                        + "\",\"exp\":"
                        + exp
                        + ",\"sub\":\"google-sub-123\",\"email\":\"user@gmail.com\"}")
                    .getBytes(StandardCharsets.UTF_8));
    String signature =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("fake-signature".getBytes(StandardCharsets.UTF_8));
    return header + "." + payload + "." + signature;
  }

  private long futureEpoch() {
    return (System.currentTimeMillis() / 1000) + 3600;
  }

  private ObjectNode createTokenInfoResponse() {
    ObjectNode tokenInfo = objectMapper.createObjectNode();
    tokenInfo.put("sub", "google-sub-123");
    tokenInfo.put("email", "user@gmail.com");
    tokenInfo.put("name", "Test User");
    tokenInfo.put("picture", "https://lh3.googleusercontent.com/photo");
    return tokenInfo;
  }
}
