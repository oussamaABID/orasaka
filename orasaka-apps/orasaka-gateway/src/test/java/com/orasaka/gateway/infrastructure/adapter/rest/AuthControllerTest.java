package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.gateway.domain.model.AuthContracts;
import com.orasaka.gateway.domain.model.AuthContracts.AuthResponse;
import com.orasaka.gateway.domain.model.AuthContracts.GenericMessageResponse;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.IdentityReconciliationService;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import com.orasaka.identity.domain.ports.inbound.PasswordRecoveryService;
import com.orasaka.identity.infrastructure.support.BadCredentialsException;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private IdentityService identityService;

  @Mock private IdentityReconciliationService reconciliationService;

  @Mock private PasswordRecoveryService passwordRecoveryService;

  private AuthController controller;

  @BeforeEach
  void setUp() {
    controller =
        new AuthController(identityService, reconciliationService, passwordRecoveryService);
  }

  @Test
  void login_validCredentials_returnsOk() {
    AuthContracts.LoginRequest request =
        new AuthContracts.LoginRequest("test@example.com", "password");
    User user =
        new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());
    when(identityService.authenticate("test@example.com", "password")).thenReturn(user);

    ResponseEntity<Object> response = controller.login(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody() instanceof AuthResponse);
    AuthResponse body = (AuthResponse) response.getBody();
    assertEquals("testuser", body.username());
  }

  @Test
  void login_invalidCredentials_returnsUnauthorized() {
    AuthContracts.LoginRequest request =
        new AuthContracts.LoginRequest("test@example.com", "password");
    when(identityService.authenticate("test@example.com", "password"))
        .thenThrow(new BadCredentialsException("Invalid"));

    ResponseEntity<Object> response = controller.login(request);

    assertNotNull(response);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertTrue(response.getBody() instanceof Map);
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals("Invalid email or password", body.get("error"));
  }

  @Test
  void oauthLogin_validToken_returnsOk() {
    AuthContracts.OAuthRequest request =
        new AuthContracts.OAuthRequest("google", "google-token", "oauth@example.com", "oauthuser");
    User user =
        new User(
            UUID.randomUUID(),
            "oauthuser",
            "oauth@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());
    when(reconciliationService.reconcile("google", "google-token")).thenReturn(user);

    ResponseEntity<Object> response = controller.oauthLogin(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody() instanceof AuthResponse);
  }

  @Test
  void oauthLogin_invalidToken_returnsUnauthorized() {
    AuthContracts.OAuthRequest request =
        new AuthContracts.OAuthRequest("google", "google-token", "oauth@example.com", "oauthuser");
    when(reconciliationService.reconcile("google", "google-token"))
        .thenThrow(new IllegalArgumentException("Invalid token"));

    ResponseEntity<Object> response = controller.oauthLogin(request);

    assertNotNull(response);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void register_successfulWithoutVerification_returnsCreated() {
    AuthContracts.RegisterRequest request =
        new AuthContracts.RegisterRequest("testuser", "test@example.com", "password", "en");
    User user =
        new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());

    when(identityService.register("testuser", "test@example.com", "password", "en"))
        .thenReturn(user);
    when(identityService.requiresEmailVerification()).thenReturn(false);

    ResponseEntity<Object> response = controller.register(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody() instanceof AuthResponse);
  }

  @Test
  void register_successfulWithVerification_returnsCreatedAndVerificationFlag() {
    AuthContracts.RegisterRequest request =
        new AuthContracts.RegisterRequest("testuser", "test@example.com", "password", "en");
    User user =
        new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());

    when(identityService.register("testuser", "test@example.com", "password", "en"))
        .thenReturn(user);
    when(identityService.requiresEmailVerification()).thenReturn(true);

    ResponseEntity<Object> response = controller.register(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody() instanceof Map);
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(true, body.get("requires_verification"));
  }

  @Test
  void verifyAccount_successful_returnsOk() {
    AuthContracts.VerifyTokenRequest request = new AuthContracts.VerifyTokenRequest("valid-token");
    when(identityService.verifyToken("valid-token")).thenReturn(true);

    ResponseEntity<Void> response = controller.verifyAccount(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void verifyAccount_failed_returnsBadRequest() {
    AuthContracts.VerifyTokenRequest request =
        new AuthContracts.VerifyTokenRequest("invalid-token");
    when(identityService.verifyToken("invalid-token")).thenReturn(false);

    ResponseEntity<Void> response = controller.verifyAccount(request);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void forgotPassword_always_returnsOk() {
    AuthContracts.ForgotPasswordRequest request =
        new AuthContracts.ForgotPasswordRequest("test@example.com");

    ResponseEntity<GenericMessageResponse> response = controller.forgotPassword(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(passwordRecoveryService).requestPasswordReset("test@example.com");
  }

  @Test
  void resetPassword_successful_returnsOk() {
    AuthContracts.ResetPasswordRequest request =
        new AuthContracts.ResetPasswordRequest("token", "newpassword");

    ResponseEntity<Object> response = controller.resetPassword(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(passwordRecoveryService).resetPassword("token", "newpassword");
  }

  @Test
  void resetPassword_invalidToken_returnsBadRequest() {
    AuthContracts.ResetPasswordRequest request =
        new AuthContracts.ResetPasswordRequest("token", "newpassword");
    doThrow(new InvalidRequestException("Invalid token"))
        .when(passwordRecoveryService)
        .resetPassword("token", "newpassword");

    ResponseEntity<Object> response = controller.resetPassword(request);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }
}
