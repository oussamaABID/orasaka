package com.orasaka.gateway.endpoint;

import com.orasaka.gateway.dto.AuthContracts;
import com.orasaka.gateway.dto.AuthResponse;
import com.orasaka.identity.config.IdentityInfrastructureProperties;
import com.orasaka.identity.domain.User;
import com.orasaka.identity.exception.BadCredentialsException;
import com.orasaka.identity.service.IdentityReconciliationService;
import com.orasaka.identity.service.IdentityService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
  private final IdentityInfrastructureProperties identityProperties;

  /**
   * Constructs the controller.
   *
   * @param identityService The identity service interface.
   * @param reconciliationService The OAuth2 identity reconciliation service interface.
   * @param identityProperties The identity config properties.
   */
  public AuthController(
      IdentityService identityService,
      IdentityReconciliationService reconciliationService,
      IdentityInfrastructureProperties identityProperties) {
    this.identityService = identityService;
    this.reconciliationService = reconciliationService;
    this.identityProperties = identityProperties;
  }

  /**
   * Login endpoint.
   *
   * @param loginRequest Credentials.
   * @return Auth response token or 401 error.
   */
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody AuthContracts.LoginRequest loginRequest) {
    logger.debug("Received login request for email: {}", loginRequest.email());
    try {
      User user = identityService.authenticate(loginRequest.email(), loginRequest.password());
      logger.debug("User with email {} authenticated successfully", loginRequest.email());
      return ResponseEntity.ok(
          new AuthResponse(
              user.id().toString(), user.username(), user.activeInterceptions()));
    } catch (BadCredentialsException ex) {
      logger.warn("Authentication failed for email: {}", loginRequest.email());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Invalid email or password"));
    }
  }

  /**
   * OAuth2 Token-Exchange login endpoint.
   *
   * <p>Receives an external identity token from the frontend (NextAuth), delegates verification
   * to the matching provider strategy, and reconciles the identity in the local database.
   *
   * @param req Token-exchange payload containing provider and idToken.
   * @return Auth response token or 401 error.
   */
  @PostMapping("/oauth")
  public ResponseEntity<?> oauthLogin(@RequestBody AuthContracts.OAuthRequest req) {
    logger.debug("Received OAuth2 token-exchange request for provider: {}", req.provider());

    try {
      User user = reconciliationService.reconcile(req.provider(), req.idToken());
      logger.debug(
          "OAuth2 user reconciled successfully: provider={}, id={}",
          req.provider(), user.id());
      return ResponseEntity.ok(
          new AuthResponse(
              user.id().toString(), user.username(), user.activeInterceptions()));
    } catch (IllegalArgumentException ex) {
      logger.warn("OAuth2 token-exchange failed for provider={}: {}", req.provider(), ex.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", ex.getMessage()));
    }
  }

  /**
   * Register endpoint.
   *
   * @param req Registration payload.
   * @return Registration response status with {@link AuthResponse} or verification info.
   */
  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody AuthContracts.RegisterRequest req) {
    logger.debug("Received registration request for email: {}", req.email());

    User created =
        identityService.register(req.username(), req.email(), req.password(), req.language());

    logger.debug("User registered successfully: {} ({})", req.username(), created.id());

    boolean requiresVerification =
        identityProperties.emailVerification() != null
            && identityProperties.emailVerification().enabled();
    if (requiresVerification) {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("requires_verification", true, "email", created.email()));
    }

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new AuthResponse(
                created.id().toString(), created.username(), created.activeInterceptions()));
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
}
