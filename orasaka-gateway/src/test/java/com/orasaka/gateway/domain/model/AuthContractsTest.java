package com.orasaka.gateway.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.gateway.domain.model.AuthContracts.AuthResponse;
import com.orasaka.gateway.domain.model.AuthContracts.RegisterResponse;
import com.orasaka.gateway.domain.model.AuthContracts.UserDescriptor;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

/** Unit tests for {@link AuthContracts}, {@link RegisterResponse}, and {@link UserDescriptor}. */
class AuthContractsTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() {}


  @Nested
  @DisplayName("LoginRequest")
  class LoginRequestTests {

    @Test
    @DisplayName("valid credentials accepted")
    void validCredentials() {
      var req = new AuthContracts.LoginRequest(TEST_EMAIL, "pass123");
      assertEquals(TEST_EMAIL, req.email());
      assertEquals("pass123", req.password());
    }

    @Test
    @DisplayName("null email throws InvalidRequestException")
    void nullEmail() {
      assertThrows(
          InvalidRequestException.class, () -> new AuthContracts.LoginRequest(null, PASS));
    }

    @Test
    @DisplayName("blank email throws InvalidRequestException")
    void blankEmail() {
      assertThrows(
          InvalidRequestException.class, () -> new AuthContracts.LoginRequest("  ", PASS));
    }

    @Test
    @DisplayName("null password throws InvalidRequestException")
    void nullPassword() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.LoginRequest(TEST_EMAIL, null));
    }

    @Test
    @DisplayName("blank password throws InvalidRequestException")
    void blankPassword() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.LoginRequest(TEST_EMAIL, "  "));
    }
  }

  @Nested
  @DisplayName("OAuthRequest")
  class OAuthRequestTests {

    @Test
    @DisplayName("valid OAuth request accepted")
    void validOAuth() {
      var req = new AuthContracts.OAuthRequest("google", "token-123", TEST_EMAIL, "display");
      assertEquals("google", req.provider());
      assertEquals("token-123", req.idToken());
      assertEquals(TEST_EMAIL, req.email());
    }

    @Test
    @DisplayName("null provider throws InvalidRequestException")
    void nullProvider() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.OAuthRequest(null, "token", TEST_EMAIL_SHORT, "name"));
    }

    @Test
    @DisplayName("blank provider throws InvalidRequestException")
    void blankProvider() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.OAuthRequest("  ", "token", TEST_EMAIL_SHORT, "name"));
    }

    @Test
    @DisplayName("null idToken throws InvalidRequestException")
    void nullIdToken() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.OAuthRequest("google", null, TEST_EMAIL_SHORT, "name"));
    }

    @Test
    @DisplayName("blank idToken throws InvalidRequestException")
    void blankIdToken() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.OAuthRequest("google", "  ", TEST_EMAIL_SHORT, "name"));
    }
  }

  @Nested
  @DisplayName("VerifyTokenRequest")
  class VerifyTokenRequestTests {

    @Test
    @DisplayName("valid token accepted")
    void validToken() {
      var req = new AuthContracts.VerifyTokenRequest("abc-123");
      assertEquals("abc-123", req.token());
    }

    @Test
    @DisplayName("null token throws InvalidRequestException")
    void nullToken() {
      assertThrows(InvalidRequestException.class, () -> new AuthContracts.VerifyTokenRequest(null));
    }

    @Test
    @DisplayName("blank token throws InvalidRequestException")
    void blankToken() {
      assertThrows(InvalidRequestException.class, () -> new AuthContracts.VerifyTokenRequest("  "));
    }
  }

  @Nested
  @DisplayName("RegisterRequest")
  class RegisterRequestTests {

    @Test
    @DisplayName("valid registration accepted")
    void validRegistration() {
      var req = new AuthContracts.RegisterRequest("user", "u@t.com", PASS, LANG_EN);
      assertEquals("user", req.username());
      assertEquals("u@t.com", req.email());
      assertEquals(PASS, req.password());
      assertEquals(LANG_EN, req.language());
    }

    @Test
    @DisplayName("null username throws InvalidRequestException")
    void nullUsername() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.RegisterRequest(null, "u@t.com", PASS, LANG_EN));
    }

    @Test
    @DisplayName("null email throws InvalidRequestException")
    void nullEmail() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.RegisterRequest("user", null, PASS, LANG_EN));
    }

    @Test
    @DisplayName("null password throws InvalidRequestException")
    void nullPassword() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.RegisterRequest("user", "u@t.com", null, LANG_EN));
    }
  }

  @Nested
  @DisplayName("RegisterResponse")
  class RegisterResponseTests {

    @Test
    @DisplayName("success factory sets user descriptor, null error")
    void successFactory() {
      var descriptor =
          new UserDescriptor(
              "550e8400-e29b-41d4-a716-446655440000",
              ADMIN,
              "a@b.com",
              List.of(ROLE_USER),
              Map.of("language", LANG_EN));
      var result = RegisterResponse.success(descriptor);
      assertSame(descriptor, result.user());
      assertNull(result.error());
    }

    @Test
    @DisplayName("failure factory sets error, null user")
    void failureFactory() {
      var result = RegisterResponse.failure("duplicate email");
      assertNull(result.user());
      assertEquals("duplicate email", result.error());
    }
  }

  @Nested
  @DisplayName("UserDescriptor")
  class UserDescriptorTests {

    @Test
    @DisplayName("valid descriptor accepted")
    void validDescriptor() {
      var desc =
          new UserDescriptor(
              "id-123", ADMIN, "a@b.com", List.of(ROLE_USER), Map.of("lang", LANG_EN));
      assertEquals("id-123", desc.id());
      assertEquals(ADMIN, desc.username());
      assertEquals(List.of(ROLE_USER), desc.authorities());
    }

    @Test
    @DisplayName("null id throws NullPointerException")
    void nullId() {
      assertThrows(
          NullPointerException.class,
          () -> new UserDescriptor(null, "name", TEST_EMAIL_SHORT, null, null));
    }

    @Test
    @DisplayName("null username throws NullPointerException")
    void nullUsername() {
      assertThrows(
          NullPointerException.class, () -> new UserDescriptor("id", null, TEST_EMAIL_SHORT, null, null));
    }

    @Test
    @DisplayName("null authorities defaults to empty list")
    void nullAuthorities() {
      var desc = new UserDescriptor("id", "name", TEST_EMAIL_SHORT, null, null);
      assertEquals(List.of(), desc.authorities());
    }
  }

  @Nested
  @DisplayName("AuthResponse")
  class AuthResponseTests {

    @Test
    @DisplayName("valid response accepted")
    void validResponse() {
      var resp =
          new AuthResponse(
              "token-123",
              ADMIN,
              "admin@orasaka.com",
              List.of("ROLE_ADMIN"),
              List.of("onboarding"));
      assertEquals("token-123", resp.token());
      assertEquals(ADMIN, resp.username());
      assertEquals("admin@orasaka.com", resp.email());
      assertEquals(List.of("ROLE_ADMIN"), resp.authorities());
      assertEquals(List.of("onboarding"), resp.activeInterceptions());
    }

    @Test
    @DisplayName("null token throws NullPointerException")
    void nullToken() {
      List<String> emptyAuthorities = List.of();
      List<String> emptyInterceptions = List.of();
      assertThrows(
          NullPointerException.class,
          () -> new AuthResponse(null, ADMIN, "a@b.com", emptyAuthorities, emptyInterceptions));
    }

    @Test
    @DisplayName("null activeInterceptions defaults to empty list")
    void nullInterceptions() {
      var resp =
          new AuthResponse("token", ADMIN, "admin@orasaka.com", List.of("ROLE_ADMIN"), null);
      assertEquals(List.of(), resp.activeInterceptions());
    }
  }
}
