package com.orasaka.gateway.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.exception.InvalidRequestException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AuthContracts}, {@link RegisterResponse}, and {@link UserDescriptor}. */
class AuthContractsTest {

  @Nested
  @DisplayName("LoginRequest")
  class LoginRequestTests {

    @Test
    @DisplayName("valid credentials accepted")
    void validCredentials() {
      var req = new AuthContracts.LoginRequest("user@test.com", "pass123");
      assertEquals("user@test.com", req.email());
      assertEquals("pass123", req.password());
    }

    @Test
    @DisplayName("null email throws InvalidRequestException")
    void nullEmail() {
      assertThrows(
          InvalidRequestException.class, () -> new AuthContracts.LoginRequest(null, "pass"));
    }

    @Test
    @DisplayName("blank email throws InvalidRequestException")
    void blankEmail() {
      assertThrows(
          InvalidRequestException.class, () -> new AuthContracts.LoginRequest("  ", "pass"));
    }

    @Test
    @DisplayName("null password throws InvalidRequestException")
    void nullPassword() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.LoginRequest("user@test.com", null));
    }

    @Test
    @DisplayName("blank password throws InvalidRequestException")
    void blankPassword() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.LoginRequest("user@test.com", "  "));
    }
  }

  @Nested
  @DisplayName("OAuthRequest")
  class OAuthRequestTests {

    @Test
    @DisplayName("valid OAuth request accepted")
    void validOAuth() {
      var req = new AuthContracts.OAuthRequest("google", "token-123", "user@test.com", "display");
      assertEquals("google", req.provider());
      assertEquals("token-123", req.idToken());
      assertEquals("user@test.com", req.email());
    }

    @Test
    @DisplayName("null provider throws InvalidRequestException")
    void nullProvider() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.OAuthRequest(null, "token", "e@t.com", "name"));
    }

    @Test
    @DisplayName("blank provider throws InvalidRequestException")
    void blankProvider() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.OAuthRequest("  ", "token", "e@t.com", "name"));
    }

    @Test
    @DisplayName("null idToken throws InvalidRequestException")
    void nullIdToken() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.OAuthRequest("google", null, "e@t.com", "name"));
    }

    @Test
    @DisplayName("blank idToken throws InvalidRequestException")
    void blankIdToken() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.OAuthRequest("google", "  ", "e@t.com", "name"));
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
      var req = new AuthContracts.RegisterRequest("user", "u@t.com", "pass", "en");
      assertEquals("user", req.username());
      assertEquals("u@t.com", req.email());
      assertEquals("pass", req.password());
      assertEquals("en", req.language());
    }

    @Test
    @DisplayName("null username throws InvalidRequestException")
    void nullUsername() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.RegisterRequest(null, "u@t.com", "pass", "en"));
    }

    @Test
    @DisplayName("null email throws InvalidRequestException")
    void nullEmail() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.RegisterRequest("user", null, "pass", "en"));
    }

    @Test
    @DisplayName("null password throws InvalidRequestException")
    void nullPassword() {
      assertThrows(
          InvalidRequestException.class,
          () -> new AuthContracts.RegisterRequest("user", "u@t.com", null, "en"));
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
              "admin",
              "a@b.com",
              List.of("ROLE_USER"),
              Map.of("language", "en"));
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
              "id-123", "admin", "a@b.com", List.of("ROLE_USER"), Map.of("lang", "en"));
      assertEquals("id-123", desc.id());
      assertEquals("admin", desc.username());
      assertEquals(List.of("ROLE_USER"), desc.authorities());
    }

    @Test
    @DisplayName("null id throws NullPointerException")
    void nullId() {
      assertThrows(
          NullPointerException.class,
          () -> new UserDescriptor(null, "name", "e@t.com", null, null));
    }

    @Test
    @DisplayName("null username throws NullPointerException")
    void nullUsername() {
      assertThrows(
          NullPointerException.class, () -> new UserDescriptor("id", null, "e@t.com", null, null));
    }

    @Test
    @DisplayName("null authorities defaults to empty list")
    void nullAuthorities() {
      var desc = new UserDescriptor("id", "name", "e@t.com", null, null);
      assertEquals(List.of(), desc.authorities());
    }
  }

  @Nested
  @DisplayName("AuthResponse")
  class AuthResponseTests {

    @Test
    @DisplayName("valid response accepted")
    void validResponse() {
      var resp = new AuthResponse("token-123", "admin", List.of("onboarding"));
      assertEquals("token-123", resp.token());
      assertEquals("admin", resp.username());
      assertEquals(List.of("onboarding"), resp.activeInterceptions());
    }

    @Test
    @DisplayName("null token throws NullPointerException")
    void nullToken() {
      assertThrows(NullPointerException.class, () -> new AuthResponse(null, "admin", List.of()));
    }

    @Test
    @DisplayName("null activeInterceptions defaults to empty list")
    void nullInterceptions() {
      var resp = new AuthResponse("token", "admin", null);
      assertEquals(List.of(), resp.activeInterceptions());
    }
  }
}
