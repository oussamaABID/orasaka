package com.orasaka.gateway.infrastructure.adapter.graphql;

import com.orasaka.gateway.domain.model.AuthContracts.RegisterResponse;
import com.orasaka.gateway.domain.model.AuthContracts.UserDescriptor;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import com.orasaka.identity.infrastructure.support.UserAlreadyExistsException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Controller that exposes identity-related queries, mutations, and schema mappings.
 *
 * <p>Extracted from {@code AiController} to enforce §2.1 (250-line limit) and separate AI
 * orchestration concerns from user identity management.
 *
 * @since 1.1.0
 */
@Controller
class IdentityController {

  private static final Logger logger = LoggerFactory.getLogger(IdentityController.class);

  private final IdentityService identityService;
  private final ExecutorService virtualThreadExecutor;

  IdentityController(IdentityService identityService, ExecutorService virtualThreadExecutor) {
    this.identityService =
        Objects.requireNonNull(identityService, "IdentityService must not be null");
    this.virtualThreadExecutor =
        Objects.requireNonNull(virtualThreadExecutor, "ExecutorService must not be null");
  }

  /**
   * Schema mapping for authorities list.
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
   * @param user The authenticated user.
   * @return me details.
   */
  @QueryMapping
  public CompletableFuture<User> me(@AuthenticationPrincipal User user) {
    logger.debug("GraphQL query me() invoked for user: {}", user.username());
    return CompletableFuture.supplyAsync(() -> user, virtualThreadExecutor);
  }

  /**
   * Mutation updating user preferences.
   *
   * @param preferences The map of preferences.
   * @param user The authenticated user.
   * @return The user.
   */
  @MutationMapping
  public CompletableFuture<User> updatePreferences(
      @Argument Map<String, Object> preferences, @AuthenticationPrincipal User user) {
    logger.debug(
        "GraphQL updatePreferences mutation invoked for user: {}, preferences: {}",
        user.username(),
        preferences);
    return CompletableFuture.supplyAsync(
        () -> identityService.updatePreferences(user.id().toString(), preferences),
        virtualThreadExecutor);
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
  public CompletableFuture<RegisterResponse> register(
      @Argument String username,
      @Argument String email,
      @Argument String password,
      @Argument String language) {
    logger.debug("GraphQL register mutation invoked for email: {}", email);
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            User created = identityService.register(username, email, password, language);
            logger.debug("User registered successfully: {} ({})", username, created.id());
            UserDescriptor descriptor =
                new UserDescriptor(
                    created.id().toString(),
                    created.username(),
                    created.email(),
                    List.copyOf(created.authorities()),
                    created.preferences());
            return RegisterResponse.success(descriptor);
          } catch (UserAlreadyExistsException ex) {
            logger.warn("Registration rejected — email already in use: {}", email);
            return RegisterResponse.failure(ex.getMessage());
          }
        },
        virtualThreadExecutor);
  }

  /**
   * Query returning interception schema.
   *
   * @param schemaId The schema ID.
   * @param user The authenticated user.
   * @return The interception schema JSON string.
   */
  @QueryMapping
  public CompletableFuture<String> interceptionSchema(
      @Argument String schemaId, @AuthenticationPrincipal User user) {
    logger.debug(
        "GraphQL query interceptionSchema() invoked for user: {}, schemaId: {}",
        user.username(),
        schemaId);
    return CompletableFuture.supplyAsync(
        () -> identityService.loadInterceptionSchema(schemaId), virtualThreadExecutor);
  }

  /**
   * Mutation resolving an active interception.
   *
   * @param interceptionType The type of interception.
   * @param schemaId The schema ID.
   * @param responses The inputs responses map.
   * @param user The authenticated user.
   * @return Resolves to true on success.
   */
  @MutationMapping
  public CompletableFuture<Boolean> resolveInterception(
      @Argument String interceptionType,
      @Argument String schemaId,
      @Argument Map<String, Object> responses,
      @AuthenticationPrincipal User user) {
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
