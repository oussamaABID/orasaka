package com.orasaka.gateway.infrastructure.adapter.graphql;

import com.orasaka.core.application.engine.GraphEngine;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.NodeState;
import com.orasaka.core.domain.model.NodeState.Active;
import com.orasaka.core.domain.model.NodeState.Invisible;
import com.orasaka.core.domain.model.NodeState.Locked;
import com.orasaka.core.domain.model.OperationGraph;
import com.orasaka.core.domain.model.audio.AudioRequest;
import com.orasaka.core.domain.model.audio.AudioResponse;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.model.image.ImageRequest;
import com.orasaka.core.domain.model.image.ImageResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.gateway.application.processing.ContextResolver;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.Base64;
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
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

/**
 * GraphQL Controller that exposes the Orasaka AI capabilities as queries, mutations, and
 * subscriptions. Identity-related operations are in {@link IdentityController}.
 *
 * @see IdentityController
 */
@Controller
public class AiController {

  private static final Logger logger = LoggerFactory.getLogger(AiController.class);

  private final AiClient aiClient;
  private final GraphEngine graphEngine;
  private final UserProfileProvider userProfileProvider;
  private final ExecutorService virtualThreadExecutor;
  private final CatalogModelManager catalogModelManager;

  /**
   * Constructs the controller.
   *
   * @param aiClient The core AI facade.
   * @param graphEngine The graph engine.
   * @param userProfileProvider The user profile provider.
   * @param virtualThreadExecutor The thread-bounded context propagation executor service.
   * @param catalogModelManager The dynamic model catalog manager.
   */
  public AiController(
      AiClient aiClient,
      GraphEngine graphEngine,
      UserProfileProvider userProfileProvider,
      ExecutorService virtualThreadExecutor,
      CatalogModelManager catalogModelManager) {
    this.aiClient = aiClient;
    this.graphEngine = graphEngine;
    this.userProfileProvider = userProfileProvider;
    this.virtualThreadExecutor = virtualThreadExecutor;
    this.catalogModelManager =
        Objects.requireNonNull(catalogModelManager, "CatalogModelManager must not be null");
  }

  /**
   * Mutation executing chat.
   *
   * @param prompt The prompt.
   * @param conversationId The conversation ID.
   * @param user The authenticated user.
   * @return The chat response.
   */
  @MutationMapping
  public CompletableFuture<ChatResponse> chat(
      @Argument String prompt,
      @Argument String conversationId,
      @AuthenticationPrincipal User user) {
    logger.debug(
        "GraphQL chat mutation invoked for conversationId: {}, prompt: {}", conversationId, prompt);
    return CompletableFuture.supplyAsync(
        () -> {
          Context context = ContextResolver.resolve(user, conversationId, userProfileProvider);
          ChatRequest request = new ChatRequest(prompt, null, null, context);
          ChatResponse response = aiClient.chat(request);
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
   * @param user The authenticated user.
   * @return The stream of responses.
   */
  @SubscriptionMapping
  public Flux<ChatResponse> chatStream(
      @Argument String prompt,
      @Argument String conversationId,
      @AuthenticationPrincipal User user) {
    logger.debug(
        "GraphQL chat stream subscription invoked for conversationId: {}, prompt: {}",
        conversationId,
        prompt);
    Context context = ContextResolver.resolve(user, conversationId, userProfileProvider);
    ChatRequest request = new ChatRequest(prompt, null, null, context);
    return aiClient.stream(request);
  }

  /**
   * Mutation generating image.
   *
   * @param prompt The image prompt.
   * @param model Optional model selection.
   * @param user The authenticated user principal.
   * @return The image URL response.
   */
  @MutationMapping
  public CompletableFuture<ChatResponse> image(
      @Argument String prompt, @Argument String model, @AuthenticationPrincipal User user) {
    logger.debug("GraphQL image mutation invoked for prompt: {}, model: {}", prompt, model);
    return CompletableFuture.supplyAsync(
        () -> {
          String activeModel = model;
          if (activeModel == null || activeModel.isBlank()) {
            activeModel =
                catalogModelManager
                    .getDefaultModelByCategory("image")
                    .map(CatalogModelDto::modelName)
                    .orElse("stable-diffusion-xl");
          }
          Context context = ContextResolver.resolve(user, null, userProfileProvider);
          ImageRequest request =
              new ImageRequest(prompt, null, null, activeModel, Map.of(), context);
          ImageResponse response = aiClient.image(request);
          return new ChatResponse(response.url(), null, Map.of("format", response.format()));
        },
        virtualThreadExecutor);
  }

  /**
   * Mutation generating speech.
   *
   * @param prompt The speech text prompt.
   * @param model Optional model selection.
   * @param voice Optional voice selection.
   * @param user The authenticated user principal.
   * @return The speech URL response.
   */
  @MutationMapping
  public CompletableFuture<ChatResponse> speech(
      @Argument String prompt,
      @Argument String model,
      @Argument String voice,
      @AuthenticationPrincipal User user) {
    logger.debug(
        "GraphQL speech mutation invoked for prompt: {}, model: {}, voice: {}",
        prompt,
        model,
        voice);
    return CompletableFuture.supplyAsync(
        () -> {
          String activeModel = model;
          if (activeModel == null || activeModel.isBlank()) {
            activeModel =
                catalogModelManager
                    .getDefaultModelByCategory("speech")
                    .map(CatalogModelDto::modelName)
                    .orElse("piper-en-medium-ryan");
          }
          String activeVoice = voice;
          if (activeVoice == null || activeVoice.isBlank()) {
            activeVoice =
                catalogModelManager
                    .getDefaultModelByCategory("speech")
                    .map(CatalogModelDto::options)
                    .map(
                        opts -> {
                          if (opts != null && !opts.isBlank()) {
                            return opts.split(",")[0];
                          }
                          return "alloy";
                        })
                    .orElse("alloy");
          }
          Context context = ContextResolver.resolve(user, null, userProfileProvider);
          AudioRequest request =
              new AudioRequest(prompt, activeVoice, activeModel, Map.of(), context);
          AudioResponse response = aiClient.audio(request);
          byte[] audioBytes = response.audioData();
          String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
          String audioUrl = "data:audio/mp3;base64," + base64Audio;
          return new ChatResponse(audioUrl, null, Map.of("format", response.format()));
        },
        virtualThreadExecutor);
  }

  /**
   * Query returning Compiled operation graph.
   *
   * @param user The authenticated user.
   * @return Compiled operation graph.
   */
  @QueryMapping
  public CompletableFuture<OperationGraph> operationGraph(@AuthenticationPrincipal User user) {
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
    return switch (state) {
      case Locked locked -> locked.reason();
      case Active a -> null;
      case Invisible i -> null;
    };
  }

  /**
   * Schema mapping for NodeState lock timestamp.
   *
   * @param state The state.
   * @return The lock timestamp.
   */
  @SchemaMapping(typeName = "NodeState", field = "lockedAt")
  public String nodeStateLockedAt(NodeState state) {
    return switch (state) {
      case Locked locked -> locked.lockedAt().toString();
      case Active a -> null;
      case Invisible i -> null;
    };
  }
}
