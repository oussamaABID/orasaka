package com.orasaka.gateway.endpoint;

import com.orasaka.identity.config.IdentityInfrastructureProperties;
import com.orasaka.identity.domain.User;
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
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

  private final IdentityService identityService;
  private final IdentityInfrastructureProperties identityProperties;

  /**
   * Constructs the controller.
   *
   * @param identityService The identity service.
   * @param identityProperties The identity config properties.
   */
  public AuthController(
      IdentityService identityService, IdentityInfrastructureProperties identityProperties) {
    this.identityService = identityService;
    this.identityProperties = identityProperties;
  }

  /**
   * Login endpoint.
   *
   * @param loginRequest Credentials.
   * @return Auth response token.
   */
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody AuthContracts.LoginRequest loginRequest) {
    logger.debug("Received login request for email: {}", loginRequest.email());
    User user = identityService.authenticate(loginRequest.email(), loginRequest.password());
    if (user == null) {
      logger.warn("Authentication failed for email: {}", loginRequest.email());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Invalid email or password"));
    }
    logger.debug("User with email {} authenticated successfully", loginRequest.email());
    return ResponseEntity.ok(
        Map.of(
            "token", user.id().toString(),
            "username", user.username(),
            "active_interceptions", user.activeInterceptions()));
  }

  /**
   * OAuth login endpoint.
   *
   * @param req OAuth properties.
   * @return Auth response token.
   */
  @PostMapping("/oauth")
  public ResponseEntity<?> oauthLogin(@RequestBody AuthContracts.OAuthRequest req) {
    logger.debug("Received OAuth2 login request for email: {}", req.email());
    String username = req.username();
    if (username == null || username.isBlank()) {
      username = req.email().split("@")[0];
    }

    User user = identityService.provisionOrAuthenticateOAuth(req.email(), username);
    if (user == null) {
      logger.warn("OAuth2 login failed for email: {}", req.email());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to authenticate or provision OAuth user"));
    }

    logger.debug(
        "OAuth2 user with email {} authenticated successfully with id: {}", req.email(), user.id());
    return ResponseEntity.ok(
        Map.of(
            "token", user.id().toString(),
            "username", user.username(),
            "active_interceptions", user.activeInterceptions()));
  }

  /**
   * Register endpoint.
   *
   * @param req Registration payload.
   * @return Registration response status.
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
            Map.of(
                "token", created.id().toString(),
                "username", created.username(),
                "active_interceptions", created.activeInterceptions()));
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
