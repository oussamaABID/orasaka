package com.orasaka.gateway.rest;

import com.orasaka.gateway.graphql.RegisterRequest;
import com.orasaka.gateway.service.ChatStreamService;
import com.orasaka.identity.config.IdentityInfrastructureProperties;
import com.orasaka.identity.domain.User;
import com.orasaka.identity.service.IdentityService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller exposing real-time Server-Sent Events (SSE) chat streaming and authentication
 * endpoints.
 */
@RestController
public class ChatStreamController {

  private static final Logger logger = LoggerFactory.getLogger(ChatStreamController.class);
  private static final String CHAT_STREAM_PATH = "/api/v1/chat/stream/{conversationId}";

  private final ChatStreamService chatStreamService;
  private final IdentityService identityService;
  private final IdentityInfrastructureProperties identityProperties;

  /**
   * Constructs the controller.
   *
   * @param chatStreamService The service orchestrating token streaming.
   * @param identityService The identity provider for authentication.
   * @param identityProperties The identity configurations.
   */
  public ChatStreamController(
      ChatStreamService chatStreamService,
      IdentityService identityService,
      IdentityInfrastructureProperties identityProperties) {
    this.chatStreamService = chatStreamService;
    this.identityService = identityService;
    this.identityProperties = identityProperties;
  }

  /**
   * Resolves the currently authenticated {@link User} from the security context.
   *
   * @return The authenticated {@link User} for the current request.
   */
  private User getCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User user) {
      return user;
    }
    throw new AccessDeniedException("Access Denied: User is not authenticated");
  }

  /**
   * Streams an AI chat response as Server-Sent Events (SSE), word-by-word.
   *
   * @param conversationId The session identifier for conversation memory isolation.
   * @param prompt The user's text prompt.
   * @return An SseEmitter instance emitting token chunks as SSE events.
   */
  @GetMapping(value = CHAT_STREAM_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamChat(@PathVariable String conversationId, @RequestParam String prompt) {
    logger.debug(
        "SSE stream request received for conversationId: {}, prompt: {}", conversationId, prompt);
    SseEmitter emitter = new SseEmitter(0L); // 0 means no timeout

    User user = getCurrentUser();
    chatStreamService.streamSse(conversationId, prompt, user, emitter);

    return emitter;
  }

  /** Authenticates a user by email and password. */
  @PostMapping("/api/v1/auth/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
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

  /** Authenticates or JIT provisions a user via OAuth2 credentials (Google / GitHub). */
  @PostMapping("/api/v1/auth/oauth")
  public ResponseEntity<?> oauthLogin(@RequestBody OAuthRequest req) {
    logger.debug("Received OAuth2 login request for email: {}", req.email());
    if (req.email() == null || req.email().isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "Email is required for OAuth login"));
    }

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

  @PostMapping("/api/v1/auth/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
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
}
