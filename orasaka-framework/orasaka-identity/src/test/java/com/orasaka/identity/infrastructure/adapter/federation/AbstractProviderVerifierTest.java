package com.orasaka.identity.infrastructure.adapter.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orasaka.identity.domain.model.ExtractedProfile;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AbstractProviderVerifierTest {

  private static class DummyProviderVerifier extends AbstractProviderVerifier {
    @Override
    protected String providerId() {
      return "dummy";
    }

    @Override
    protected String tokenLabel() {
      return "Dummy token";
    }

    @Override
    protected ExtractedProfile doVerifyAndExtract(String idToken) {
      return new ExtractedProfile("dummy-email", "dummy-sub", "Dummy User", "http://avatar");
    }
  }

  @Test
  void testSupports() {
    DummyProviderVerifier verifier = new DummyProviderVerifier();
    assertThat(verifier.supports("dummy")).isTrue();
    assertThat(verifier.supports("DUMMY")).isTrue();
    assertThat(verifier.supports("other")).isFalse();
  }

  @Test
  void testVerifyAndExtractValid() {
    DummyProviderVerifier verifier = new DummyProviderVerifier();
    ExtractedProfile profile = verifier.verifyAndExtract("valid-token");
    assertThat(profile).isNotNull();
    assertThat(profile.providerId()).isEqualTo("dummy-sub");
    assertThat(profile.email()).isEqualTo("dummy-email");
    assertThat(profile.name()).isEqualTo("Dummy User");
    assertThat(profile.avatarUrl()).isEqualTo("http://avatar");
  }

  @Test
  void testVerifyAndExtractInvalid() {
    DummyProviderVerifier verifier = new DummyProviderVerifier();
    assertThatThrownBy(() -> verifier.verifyAndExtract(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Dummy token cannot be null or empty");

    assertThatThrownBy(() -> verifier.verifyAndExtract("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Dummy token cannot be null or empty");
  }

  @Test
  void testGithubProviderVerifier_supports() {
    GithubProviderVerifier verifier = new GithubProviderVerifier(RestClient.create());
    assertThat(verifier.supports("github")).isTrue();
    assertThat(verifier.tokenLabel()).isEqualTo("GitHub access token");
  }

  @Test
  void testGoogleProviderVerifier_supports() {
    GoogleProviderVerifier verifier =
        new GoogleProviderVerifier(
            RestClient.create(), new com.fasterxml.jackson.databind.ObjectMapper());
    assertThat(verifier.supports("google")).isTrue();
    assertThat(verifier.tokenLabel()).isEqualTo("Google ID token");
  }
}
