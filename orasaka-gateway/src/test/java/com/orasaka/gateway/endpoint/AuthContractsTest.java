package com.orasaka.gateway.endpoint;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.domain.User;
import com.orasaka.identity.exception.InvalidRequestException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AuthContracts} — all nested request/result records. */
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
      var req = new AuthContracts.OAuthRequest("user@test.com", "display");
      assertEquals("user@test.com", req.email());
    }

    @Test
    @DisplayName("null email throws InvalidRequestException")
    void nullEmail() {
      assertThrows(
          InvalidRequestException.class, () -> new AuthContracts.OAuthRequest(null, "display"));
    }

    @Test
    @DisplayName("blank email throws InvalidRequestException")
    void blankEmail() {
      assertThrows(
          InvalidRequestException.class, () -> new AuthContracts.OAuthRequest("  ", "display"));
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
  @DisplayName("RegisterResult")
  class RegisterResultTests {

    @Test
    @DisplayName("success factory sets user, null error")
    void successFactory() {
      var user = new User(UUID.randomUUID(), "admin", "a@b.com", true, null, null);
      var result = AuthContracts.RegisterResult.success(user);
      assertSame(user, result.user());
      assertNull(result.error());
    }

    @Test
    @DisplayName("failure factory sets error, null user")
    void failureFactory() {
      var result = AuthContracts.RegisterResult.failure("duplicate email");
      assertNull(result.user());
      assertEquals("duplicate email", result.error());
    }
  }
}
