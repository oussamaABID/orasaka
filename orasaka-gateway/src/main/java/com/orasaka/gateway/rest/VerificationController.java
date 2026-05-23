package com.orasaka.gateway.rest;

import com.orasaka.identity.service.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing REST endpoints for user account email verification. Actives only if {@code
 * orasaka.infrastructure.identity.email-verification.enabled} is {@code true}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(
    name = "orasaka.infrastructure.identity.email-verification.enabled",
    havingValue = "true")
public class VerificationController {

  private static final Logger logger = LoggerFactory.getLogger(VerificationController.class);
  private final IdentityService identityService;

  /**
   * Constructs the controller.
   *
   * @param identityService The identity service.
   */
  public VerificationController(IdentityService identityService) {
    this.identityService = identityService;
  }

  /**
   * Verifies the provided account activation token.
   *
   * @param request The verification token payload.
   * @return A ResponseEntity signaling success or verification failure.
   */
  @PostMapping("/verify")
  public ResponseEntity<Void> verifyAccount(@RequestBody VerifyTokenRequest request) {
    logger.debug("Received account verification request");
    if (request.token() == null || request.token().isBlank()) {
      return ResponseEntity.badRequest().build();
    }

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
   * Request payload for verifying account tokens.
   *
   * @param token The token string.
   * @return The VerifyTokenRequest instance.
   */
  public record VerifyTokenRequest(String token) {}
}
