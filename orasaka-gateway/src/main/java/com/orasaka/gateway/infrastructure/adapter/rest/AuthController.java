package com.orasaka.gateway.infrastructure.adapter.rest;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Package-private REST controller exposing authentication endpoints, including login, registration,
 * email verification, and OAuth2 credential provision.
 *
 * <p>Returns strongly-typed {@link AuthResponse} DTOs instead of raw {@code Map.of()} responses
 * (ERR-106). Authentication failures are handled via exception catching, not null-checking.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

  private final IdentityService identityService;
  private final IdentityReconciliationService reconciliationService;
  private final PasswordRecoveryService passwordRecoveryService;

  /**
   * Constructs the controller.
   *
   * @param identityService The identity service interface.
   * @param reconciliationService The OAuth2 identity reconciliation service interface.
   * @param passwordRecoveryService The password recovery service interface.
   */
  public AuthController(
      IdentityService identityService,
      IdentityReconciliationService reconciliationService,
      PasswordRecoveryService passwordRecoveryService) {
    this.identityService = identityService;
    this.reconciliationService = reconciliationService;
    this.passwordRecoveryService = passwordRecoveryService;
  }

  /**
   * Login endpoint.
   *
   * @param loginRequest Credentials.
   * @return Auth response token or 401 error.
   */
  @PostMapping("/login")
  public ResponseEntity<Object> login(@RequestBody AuthContracts.LoginRequest loginRequest) {
    logger.debug("Received login request for email: {}", loginRequest.email());
    try {
      User user = identityService.authenticate(loginRequest.email(), loginRequest.password());
      logger.debug("User with email {} authenticated successfully", loginRequest.email());
      return ResponseEntity.ok(
          new AuthResponse(
              user.id().toString(),
              user.username(),
              user.email(),
              List.copyOf(user.authorities()),
              user.activeInterceptions()));
    } catch (BadCredentialsException ex) {
      logger.warn("Authentication failed for email: {}", loginRequest.email());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Invalid email or password"));
    }
  }

  /**
   * OAuth2 Token-Exchange login endpoint.
   *
   * <p>Receives an external identity token from the frontend (NextAuth), delegates verification to
   * the matching provider strategy, and reconciles the identity in the local database.
   *
   * @param req Token-exchange payload containing provider and idToken.
   * @return Auth response token or 401 error.
   */
  @PostMapping("/oauth")
  public ResponseEntity<Object> oauthLogin(@RequestBody AuthContracts.OAuthRequest req) {
    logger.debug("Received OAuth2 token-exchange request for provider: {}", req.provider());

    try {
      User user = reconciliationService.reconcile(req.provider(), req.idToken());
      logger.debug(
          "OAuth2 user reconciled successfully: provider={}, id={}", req.provider(), user.id());
      return ResponseEntity.ok(
          new AuthResponse(
              user.id().toString(),
              user.username(),
              user.email(),
              List.copyOf(user.authorities()),
              user.activeInterceptions()));
    } catch (IllegalArgumentException ex) {
      logger.warn(
          "OAuth2 token-exchange failed for provider={}: {}", req.provider(), ex.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }
  }

  /**
   * Register endpoint.
   *
   * @param req Registration payload.
   * @return Registration response status with {@link AuthResponse} or verification info.
   */
  @PostMapping("/register")
  public ResponseEntity<Object> register(@RequestBody AuthContracts.RegisterRequest req) {
    logger.debug("Received registration request for email: {}", req.email());

    User created =
        identityService.register(req.username(), req.email(), req.password(), req.language());

    logger.debug("User registered successfully: {} ({})", req.username(), created.id());

    if (identityService.requiresEmailVerification()) {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("requires_verification", true, "email", created.email()));
    }

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new AuthResponse(
                created.id().toString(),
                created.username(),
                created.email(),
                List.copyOf(created.authorities()),
                created.activeInterceptions()));
  }

  /**
   * Verifies the provided account activation token.
   *
   * @param request The verification token payload.
   * @return A ResponseEntity signaling success or verification failure.
   */
  @PostMapping("/verify")
  public ResponseEntity<Void> verifyAccount(@RequestBody AuthContracts.VerifyTokenRequest request) {
    logger.debug("Received account verification request");
    boolean success = identityService.verifyToken(request.token());
    if (success) {
      logger.info("Account verified successfully with token");
      return ResponseEntity.ok().build();
    } else {
      logger.warn("Account verification failed for token");
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Forgot password endpoint. Always returns a generic success message to prevent enumeration.
   *
   * @param request The forgot password payload containing the email.
   * @return A generic response indicating the request was processed.
   */
  @PostMapping("/forgot")
  public ResponseEntity<GenericMessageResponse> forgotPassword(
      @RequestBody AuthContracts.ForgotPasswordRequest request) {
    logger.debug("Received forgot password request for email: {}", request.email());
    passwordRecoveryService.requestPasswordReset(request.email());
    return ResponseEntity.ok(
        new GenericMessageResponse(
            "If this account exists, a secure recovery email has been staged."));
  }

  /**
   * Reset password endpoint. Validates the token and updates the password.
   *
   * @param request The reset payload containing the token and new password.
   * @return 200 OK on success, 400 Bad Request on validation failure.
   */
  @PostMapping("/reset")
  public ResponseEntity<Object> resetPassword(
      @RequestBody AuthContracts.ResetPasswordRequest request) {
    logger.debug("Received password reset request");
    try {
      passwordRecoveryService.resetPassword(request.token(), request.newPassword());
      logger.info("Password reset completed successfully");
      return ResponseEntity.ok(new GenericMessageResponse("Password has been reset successfully."));
    } catch (InvalidRequestException ex) {
      logger.warn("Password reset failed: {}", ex.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }
}
