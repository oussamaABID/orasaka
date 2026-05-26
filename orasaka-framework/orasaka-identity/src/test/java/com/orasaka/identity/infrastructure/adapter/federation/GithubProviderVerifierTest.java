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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class GithubProviderVerifierTest {

  private RestClient restClient;
  private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
  private RestClient.RequestHeadersSpec<?> requestHeadersSpec;
  private RestClient.ResponseSpec responseSpec;
  private GithubProviderVerifier verifier;

  @BeforeEach
  void setUp() {
    restClient = mock(RestClient.class);
    requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    responseSpec = mock(RestClient.ResponseSpec.class);

    doReturn(requestHeadersUriSpec).when(restClient).get();
    doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(String.class));
    doReturn(requestHeadersSpec).when(requestHeadersSpec).header(any(), any());
    doReturn(requestHeadersSpec).when(requestHeadersSpec).accept(any(MediaType.class));
    doReturn(responseSpec).when(requestHeadersSpec).retrieve();

    verifier = new GithubProviderVerifier(restClient);
  }

  @Test
  void supports_github_returnsTrue() {
    assertThat(verifier.supports("github")).isTrue();
    assertThat(verifier.supports("GITHUB")).isTrue();
  }

  @Test
  void supports_other_returnsFalse() {
    assertThat(verifier.supports("google")).isFalse();
  }

  @Test
  void tokenLabel_returnsGithubAccessToken() {
    assertThat(verifier.tokenLabel()).isEqualTo("GitHub access token");
  }

  @Test
  void doVerifyAndExtract_fullProfile_returnsExtractedProfile() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode user = mapper.createObjectNode();
    user.put("id", 12345);
    user.put("login", "octocat");
    user.put("email", "octocat@github.com");
    user.put("name", "The Octocat");
    user.put("avatar_url", "https://avatars.githubusercontent.com/u/12345");

    when(responseSpec.body(JsonNode.class)).thenReturn(user);

    ExtractedProfile profile = verifier.doVerifyAndExtract("ghp_valid_token");

    assertThat(profile.email()).isEqualTo("octocat@github.com");
    assertThat(profile.providerId()).isEqualTo("12345");
    assertThat(profile.name()).isEqualTo("The Octocat");
    assertThat(profile.avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/12345");
  }

  @Test
  void doVerifyAndExtract_privateEmail_fallsBackToNoreply() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode user = mapper.createObjectNode();
    user.put("id", 67890);
    user.put("login", "privateuser");
    user.putNull("email");
    user.put("name", "Private User");
    user.putNull("avatar_url");

    when(responseSpec.body(JsonNode.class)).thenReturn(user);

    ExtractedProfile profile = verifier.doVerifyAndExtract("ghp_private_token");

    assertThat(profile.email()).isEqualTo("privateuser@users.noreply.github.com");
    assertThat(profile.providerId()).isEqualTo("67890");
    assertThat(profile.name()).isEqualTo("Private User");
    assertThat(profile.avatarUrl()).isNull();
  }

  @Test
  void doVerifyAndExtract_noName_fallsBackToLogin() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode user = mapper.createObjectNode();
    user.put("id", 99999);
    user.put("login", "noname");
    user.put("email", "noname@example.com");
    user.putNull("name");
    user.putNull("avatar_url");

    when(responseSpec.body(JsonNode.class)).thenReturn(user);

    ExtractedProfile profile = verifier.doVerifyAndExtract("ghp_noname_token");

    assertThat(profile.email()).isEqualTo("noname@example.com");
    assertThat(profile.name()).isEqualTo("noname");
  }

  @Test
  void doVerifyAndExtract_blankEmail_fallsBackToNoreply() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode user = mapper.createObjectNode();
    user.put("id", 11111);
    user.put("login", "blankmail");
    user.put("email", "   ");
    user.put("name", "Blank Mail");
    user.putNull("avatar_url");

    when(responseSpec.body(JsonNode.class)).thenReturn(user);

    ExtractedProfile profile = verifier.doVerifyAndExtract("ghp_blank_token");

    assertThat(profile.email()).isEqualTo("blankmail@users.noreply.github.com");
  }

  @Test
  void doVerifyAndExtract_nullResponse_throwsIllegalArgument() {
    when(responseSpec.body(JsonNode.class)).thenReturn(null);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract("ghp_bad_token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GitHub API returned null response");
  }

  @Test
  void verifyAndExtract_nullToken_throwsIllegalArgument() {
    assertThatThrownBy(() -> verifier.verifyAndExtract(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GitHub access token cannot be null or empty");
  }

  @Test
  void verifyAndExtract_blankToken_throwsIllegalArgument() {
    assertThatThrownBy(() -> verifier.verifyAndExtract("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GitHub access token cannot be null or empty");
  }
}
