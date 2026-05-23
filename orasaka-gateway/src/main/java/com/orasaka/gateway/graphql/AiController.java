package com.orasaka.gateway.graphql;

import com.orasaka.core.client.OrasakaAiClient;
import com.orasaka.core.context.OrasakaContext;
import com.orasaka.core.model.OrasakaChatRequest;
import com.orasaka.core.model.OrasakaChatResponse;
import com.orasaka.gateway.service.ChatStreamService;
import com.orasaka.identity.config.IdentityInfrastructureProperties;
import com.orasaka.identity.domain.User;
import com.orasaka.identity.service.IdentityService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 *
 * <p>Acts as the Backend-For-Frontend (BFF) orchestrator: resolves the authenticated user from the
 * {@link SecurityContextHolder}, constructs an immutable {@link OrasakaContext}, and dispatches
 * requests to the {@link OrasakaAiClient} facade. All operations are executed asynchronously on
 * Java 21 Virtual Threads to prevent blocking the NIO event loop.
 *
 * <p>Browser clients must never call the AI Gateway or Ollama ports directly; all traffic must be
 * routed through this controller (AGENTS.md §1.D — BFF Topology Directive).
 *
 * @see OrasakaAiClient
 * @see com.orasaka.core.context.OrasakaContext
 * @see com.orasaka.gateway.config.OrasakaSecurityFilter
 */
@Controller
public class AiController {

  private static final Logger logger = LoggerFactory.getLogger(AiController.class);

  private final OrasakaAiClient aiClient;
  private final IdentityService identityService;
  private final ChatStreamService chatStreamService;
  private final IdentityInfrastructureProperties identityProperties;
  private final ResourceLoader resourceLoader;
  private final ExecutorService virtualThreadExecutor;

  /**
   * Constructs the controller and initializes a dedicated Virtual Thread executor for non-blocking
   * GraphQL resolution.
   *
   * @param aiClient The core AI facade for dispatching chat, image, and speech requests.
   * @param identityService The service used to resolve user profiles and preferences.
   * @param chatStreamService The service orchestrating streaming chats.
   * @param identityProperties The properties for configuring identity services.
   * @param resourceLoader The resource loader for reading schema files.
   */
  public AiController(
      OrasakaAiClient aiClient,
      IdentityService identityService,
      ChatStreamService chatStreamService,
      IdentityInfrastructureProperties identityProperties,
      ResourceLoader resourceLoader) {
    this.aiClient = aiClient;
    this.identityService = identityService;
    this.chatStreamService = chatStreamService;
    this.identityProperties = identityProperties;
    this.resourceLoader = resourceLoader;
    this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Resolves the currently authenticated {@link User} from the security context.
   *
   * @return The resolved {@link User} for this request.
   * @throws AccessDeniedException if the user is unauthenticated.
   */
  private User getCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User user) {
      return user;
    }
    throw new AccessDeniedException("Access Denied: User is not authenticated");
  }

  /**
   * GraphQL schema mapping that resolves the User.authorities field into a list of strings.
   *
   * @param user The user object to resolve authorities for.
   * @return A list of authority names as strings.
   */
  @SchemaMapping(typeName = "User", field = "authorities")
  public List<String> authorities(User user) {
    return user.authorities().stream().toList();
  }

  /**
   * GraphQL query that returns the profile of the currently authenticated user.
   *
   * <p>Executed asynchronously on a Virtual Thread to remain non-blocking.
   *
   * @return A {@link CompletableFuture} resolving to the authenticated {@link User}.
   */
  @QueryMapping
  public CompletableFuture<User> me() {
    User user = getCurrentUser();
    logger.debug("GraphQL query me() invoked for user: {}", user.username());
    return CompletableFuture.supplyAsync(() -> user, virtualThreadExecutor);
  }

  /**
   * GraphQL mutation that merges new preference entries into the current user's profile.
   *
   * <p>Executes the preference update and reloads the user record in a single Virtual Thread task,
   * ensuring the returned {@link User} always reflects the persisted state.
   *
   * @param preferences A map of preference keys and values to merge (e.g., {@code tts-voice}).
   * @return A {@link CompletableFuture} resolving to the updated {@link User} record.
   * @see com.orasaka.identity.service.IdentityService#updatePreferences(String, Map)
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
   * GraphQL mutation for single-turn chat with the active AI provider.
   *
   * <p>Constructs an immutable {@link OrasakaContext} from the authenticated user and dispatches
   * the request to the {@link OrasakaAiClient}. The engine applies RAG, MCP context injection, and
   * tool attachment before calling the underlying model. All processing is non-blocking via a
   * dedicated Virtual Thread.
   *
   * @param prompt The user's text input.
   * @param conversationId The session identifier for conversation memory resolution.
   * @return A {@link CompletableFuture} resolving to the {@link OrasakaChatResponse}.
   * @see OrasakaAiClient#chat(OrasakaChatRequest)
   * @see com.orasaka.core.interceptors.memory.OrasakaMemoryResolver
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

          Set<String> roles = user.authorities();

          // Build immutable context with user preferences and raw roles
          OrasakaContext context =
              new OrasakaContext(userId, conversationId, user.preferences(), roles);

          // Dispatch to Core via facade — engine handles RAG, MCP, and tool attachment
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
   * GraphQL subscription that streams the AI response token-by-token as a reactive {@link Flux}.
   *
   * <p>The response is piped natively and event-driven from the downstream client down to the
   * subscription network emitter without artificial delays or sleeps.
   *
   * @param prompt The user's text input.
   * @param conversationId The session identifier for conversation memory isolation.
   * @return A {@link Flux} of {@link OrasakaChatResponse} objects emitted reactively.
   * @see com.orasaka.gateway.service.ChatStreamService#streamGraphQL(String, String, User)
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
   * GraphQL mutation for self-service user registration.
   *
   * <p>This endpoint is intentionally <strong>public</strong> (no security context required). It
   * delegates to {@link IdentityService#register} which validates uniqueness, BCrypt-encodes the
   * password, and inserts the user with {@code ROLE_USER}. The result is a discriminated union —
   * either a fully resolved {@link User} on success or an {@code error} string on a business-level
   * rejection (e.g., duplicate email).
   *
   * @param username The desired display name.
   * @param email The user's email (must be unique in the system).
   * @param password The plaintext password — will be BCrypt-encoded server-side.
   * @param language The preferred UI language code (e.g., {@code "en"}, {@code "fr"}).
   * @return A {@link CompletableFuture} resolving to a {@link RegisterResult}.
   */
  @MutationMapping
  public CompletableFuture<RegisterResult> register(
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
            return RegisterResult.failure("An account with this email already exists.");
          }
          logger.debug("User registered successfully: {} ({})", username, created.id());
          return RegisterResult.success(created);
        },
        virtualThreadExecutor);
  }

  /**
   * Retrieves the JSON configuration schema mapping for a specific interception block.
   *
   * @param schemaId The identifier of the interception schema.
   * @return A CompletableFuture resolving to the raw JSON schema content.
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
   * Resolves an active interception, merging the responses into the user preferences and clearing
   * the block.
   *
   * @param interceptionType The type of interception workflow.
   * @param schemaId The configuration schema ID.
   * @param responses The input responses to save.
   * @return A CompletableFuture resolving to true if resolution succeeded.
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
}
