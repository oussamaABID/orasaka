package com.orasaka.gateway.endpoint;

import com.orasaka.core.client.OrasakaAiClient;
import com.orasaka.core.context.OrasakaContext;
import com.orasaka.core.engine.NodeState;
import com.orasaka.core.engine.NodeState.Active;
import com.orasaka.core.engine.NodeState.Invisible;
import com.orasaka.core.engine.NodeState.Locked;
import com.orasaka.core.engine.OrasakaGraphEngine;
import com.orasaka.core.engine.OrasakaOperationGraph;
import com.orasaka.core.identity.OrasakaAuthority;
import com.orasaka.core.model.OrasakaChatRequest;
import com.orasaka.core.model.OrasakaChatResponse;
import com.orasaka.core.model.OrasakaImageRequest;
import com.orasaka.core.model.OrasakaImageResponse;
import com.orasaka.core.model.OrasakaSpeechRequest;
import com.orasaka.gateway.service.ChatStreamService;
import com.orasaka.identity.config.IdentityInfrastructureProperties;
import com.orasaka.identity.domain.User;
import com.orasaka.identity.service.IdentityService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;

/**
 * GraphQL Controller that exposes the Orasaka AI capabilities as queries, mutations, and
 * subscriptions.
 */
@Controller
public class AiController {

  private static final Logger logger = LoggerFactory.getLogger(AiController.class);

  private final OrasakaAiClient aiClient;
  private final IdentityService identityService;
  private final ChatStreamService chatStreamService;
  private final IdentityInfrastructureProperties identityProperties;
  private final ResourceLoader resourceLoader;
  private final OrasakaGraphEngine graphEngine;
  private final ExecutorService virtualThreadExecutor;

  /**
   * Constructs the controller.
   *
   * @param aiClient The core AI facade.
   * @param identityService The identity service.
   * @param chatStreamService The streaming chat service.
   * @param identityProperties The identity config properties.
   * @param resourceLoader The resource loader.
   * @param graphEngine The graph engine.
   */
  public AiController(
      OrasakaAiClient aiClient,
      IdentityService identityService,
      ChatStreamService chatStreamService,
      IdentityInfrastructureProperties identityProperties,
      ResourceLoader resourceLoader,
      OrasakaGraphEngine graphEngine) {
    this.aiClient = aiClient;
    this.identityService = identityService;
    this.chatStreamService = chatStreamService;
    this.identityProperties = identityProperties;
    this.resourceLoader = resourceLoader;
    this.graphEngine = graphEngine;
    this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  private User getCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User user) {
      return user;
    }
    throw new AccessDeniedException("Access Denied: User is not authenticated");
  }

  /**
   * Schemamapping for authorities list.
   *
   * @param user The user.
   * @return The authorities list.
   */
  @SchemaMapping(typeName = "User", field = "authorities")
  public List<String> authorities(User user) {
    return user.authorities().stream().toList();
  }

  /**
   * Query me details.
   *
   * @return me details.
   */
  @QueryMapping
  public CompletableFuture<User> me() {
    User user = getCurrentUser();
    logger.debug("GraphQL query me() invoked for user: {}", user.username());
    return CompletableFuture.supplyAsync(() -> user, virtualThreadExecutor);
  }

  /**
   * Mutation updating user preferences.
   *
   * @param preferences The map of preferences.
   * @return The user.
   */
  @MutationMapping
  public CompletableFuture<User> updatePreferences(@Argument Map<String, Object> preferences) {
    User user = getCurrentUser();
    logger.debug(
        "GraphQL updatePreferences mutation invoked for user: {}, preferences: {}",
        user.username(),
        preferences);
    return CompletableFuture.supplyAsync(
        () -> identityService.updatePreferences(user.id().toString(), preferences),
        virtualThreadExecutor);
  }

  /**
   * Mutation executing chat.
   *
   * @param prompt The prompt.
   * @param conversationId The conversation ID.
   * @return The chat response.
   */
  @MutationMapping
  public CompletableFuture<OrasakaChatResponse> chat(
      @Argument String prompt, @Argument String conversationId) {
    User user = getCurrentUser();
    logger.debug(
        "GraphQL chat mutation invoked for conversationId: {}, prompt: {}", conversationId, prompt);
    return CompletableFuture.supplyAsync(
        () -> {
          String userId = user.id().toString();
          Set<OrasakaAuthority> authorities =
              user.authorities().stream()
                  .map(OrasakaAuthority::new)
                  .collect(java.util.stream.Collectors.toSet());
          OrasakaContext context =
              new OrasakaContext(userId, conversationId, user.preferences(), authorities);
          OrasakaChatRequest request = new OrasakaChatRequest(prompt, null, null, context);
          OrasakaChatResponse response = aiClient.chat(request);
          logger.debug(
              "GraphQL chat mutation completed for conversationId: {}, response length: {} chars",
              conversationId,
              response.content().length());
          return response;
        },
        virtualThreadExecutor);
  }

  /**
   * Subscription streaming chat response.
   *
   * @param prompt The prompt.
   * @param conversationId The conversation ID.
   * @return The stream of responses.
   */
  @SubscriptionMapping
  public Flux<OrasakaChatResponse> chatStream(
      @Argument String prompt, @Argument String conversationId) {
    User user = getCurrentUser();
    logger.debug(
        "GraphQL chat stream subscription invoked for conversationId: {}, prompt: {}",
        conversationId,
        prompt);
    return chatStreamService.streamGraphQL(conversationId, prompt, user);
  }

  /**
   * Mutation performing user registration.
   *
   * @param username The username.
   * @param email The email address.
   * @param password The plaintext password.
   * @param language The BCP language tag.
   * @return The registration result.
   */
  @MutationMapping
  public CompletableFuture<AuthContracts.RegisterResult> register(
      @Argument String username,
      @Argument String email,
      @Argument String password,
      @Argument String language) {
    logger.debug("GraphQL register mutation invoked for email: {}", email);
    return CompletableFuture.supplyAsync(
        () -> {
          User created = identityService.register(username, email, password, language);
          if (created == null) {
            logger.warn("Registration rejected — email already in use: {}", email);
            return AuthContracts.RegisterResult.failure(
                "An account with this email already exists.");
          }
          logger.debug("User registered successfully: {} ({})", username, created.id());
          return AuthContracts.RegisterResult.success(created);
        },
        virtualThreadExecutor);
  }

  /**
   * Query returning interception schema.
   *
   * @param schemaId The schema ID.
   * @return The interception schema JSON string.
   */
  @QueryMapping
  public CompletableFuture<String> interceptionSchema(@Argument String schemaId) {
    User user = getCurrentUser();
    logger.debug(
        "GraphQL query interceptionSchema() invoked for user: {}, schemaId: {}",
        user.username(),
        schemaId);
    return CompletableFuture.supplyAsync(
        () -> {
          Map<String, String> schemas =
              identityProperties.interceptions() != null
                  ? identityProperties.interceptions().schemas()
                  : null;
          if (schemas == null || !schemas.containsKey(schemaId)) {
            logger.warn("Interception schema path not configured for: {}", schemaId);
            return null;
          }

          String path = schemas.get(schemaId);
          Resource resource = resourceLoader.getResource(path);
          if (!resource.exists()) {
            logger.error("Configured interception schema file does not exist at: {}", path);
            return null;
          }

          try (InputStream is = resource.getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
          } catch (IOException e) {
            logger.error("Failed to read schema file at: {}", path, e);
            throw new RuntimeException("Failed to read schema file", e);
          }
        },
        virtualThreadExecutor);
  }

  /**
   * Mutation resolving an active interception.
   *
   * @param interceptionType The type of interception.
   * @param schemaId The schema ID.
   * @param responses The inputs responses map.
   * @return Resolves to true on success.
   */
  @MutationMapping
  public CompletableFuture<Boolean> resolveInterception(
      @Argument String interceptionType,
      @Argument String schemaId,
      @Argument Map<String, Object> responses) {
    User user = getCurrentUser();
    logger.debug(
        "GraphQL resolveInterception mutation invoked for user: {}, type: {}, schema: {}",
        user.username(),
        interceptionType,
        schemaId);
    return CompletableFuture.supplyAsync(
        () -> {
          identityService.resolveInterception(user.id(), interceptionType, schemaId, responses);
          return true;
        },
        virtualThreadExecutor);
  }

  /**
   * Mutation generating image.
   *
   * @param prompt The image prompt.
   * @return The image URL response.
   */
  @MutationMapping
  public CompletableFuture<OrasakaChatResponse> image(@Argument String prompt) {
    User user = getCurrentUser();
    logger.debug("GraphQL image mutation invoked for prompt: {}", prompt);
    return CompletableFuture.supplyAsync(
        () -> {
          Set<OrasakaAuthority> authorities =
              user.authorities().stream()
                  .map(OrasakaAuthority::new)
                  .collect(java.util.stream.Collectors.toSet());
          OrasakaContext context =
              new OrasakaContext(user.id().toString(), null, user.preferences(), authorities);
          OrasakaImageRequest request = new OrasakaImageRequest(prompt, null, null, null, context);
          OrasakaImageResponse response = aiClient.generateImage(request);
          return new OrasakaChatResponse(response.url(), null, Map.of("format", response.format()));
        },
        virtualThreadExecutor);
  }

  /**
   * Mutation generating speech.
   *
   * @param prompt The speech text prompt.
   * @return The speech URL response.
   */
  @MutationMapping
  public CompletableFuture<OrasakaChatResponse> speech(@Argument String prompt) {
    User user = getCurrentUser();
    logger.debug("GraphQL speech mutation invoked for prompt: {}", prompt);
    return CompletableFuture.supplyAsync(
        () -> {
          Set<OrasakaAuthority> authorities =
              user.authorities().stream()
                  .map(OrasakaAuthority::new)
                  .collect(java.util.stream.Collectors.toSet());
          OrasakaContext context =
              new OrasakaContext(user.id().toString(), null, user.preferences(), authorities);
          OrasakaSpeechRequest request = new OrasakaSpeechRequest(prompt, null, context);
          byte[] audioBytes = aiClient.generateSpeech(request);
          String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
          String audioUrl = "data:audio/mp3;base64," + base64Audio;
          return new OrasakaChatResponse(audioUrl, null, Map.of("format", "mp3"));
        },
        virtualThreadExecutor);
  }

  /**
   * Query returning Compiled operation graph.
   *
   * @return Compiled operation graph.
   */
  @QueryMapping
  public CompletableFuture<OrasakaOperationGraph> operationGraph() {
    User user = getCurrentUser();
    logger.debug("GraphQL query operationGraph() invoked for user: {}", user.username());
    return CompletableFuture.supplyAsync(graphEngine::compileGraph, virtualThreadExecutor);
  }

  /**
   * Schema mapping for NodeState type.
   *
   * @param state The state.
   * @return The type string.
   */
  @SchemaMapping(typeName = "NodeState", field = "type")
  public String nodeStateType(NodeState state) {
    return switch (state) {
      case Active a -> "ACTIVE";
      case Locked l -> "LOCKED";
      case Invisible i -> "INVISIBLE";
    };
  }

  /**
   * Schema mapping for NodeState reason.
   *
   * @param state The state.
   * @return The lock reason.
   */
  @SchemaMapping(typeName = "NodeState", field = "reason")
  public String nodeStateReason(NodeState state) {
    if (state instanceof Locked locked) {
      return locked.reason();
    }
    return null;
  }

  /**
   * Schema mapping for NodeState lock timestamp.
   *
   * @param state The state.
   * @return The lock timestamp.
   */
  @SchemaMapping(typeName = "NodeState", field = "lockedAt")
  public String nodeStateLockedAt(NodeState state) {
    if (state instanceof Locked locked) {
      return locked.lockedAt().toString();
    }
    return null;
  }
}
