package com.orasaka.gateway.service;

import com.orasaka.core.engine.OrasakaAiClient;
import com.orasaka.core.support.OrasakaChatRequest;
import com.orasaka.core.support.OrasakaChatResponse;
import com.orasaka.core.support.OrasakaContext;
import com.orasaka.identity.domain.User;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * Service orchestrating AI chat streaming flows for SSE and GraphQL subscriptions.
 *
 * <p>Implements the strict Service-Controller boundary by decoupling controllers from execution
 * thread pools, payload orchestration, and context layout assembly.
 *
 * <p>All asynchronous streaming flows are initiated and processed utilizing Java 21 Virtual
 * Threads.
 *
 * @see OrasakaAiClient
 * @see OrasakaContext
 * @see SseEmitter
 */
@Service
public class ChatStreamService {

  private static final Logger logger = LoggerFactory.getLogger(ChatStreamService.class);

  private final OrasakaAiClient aiClient;
  private final ExecutorService virtualThreadExecutor;

  /**
   * Initializes the service with the AI client and a virtual thread executor.
   *
   * @param aiClient The core AI client facade.
   */
  public ChatStreamService(OrasakaAiClient aiClient) {
    this.aiClient = aiClient;
    this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Subscribes to the reactive AI token stream and emits Server-Sent Events (SSE).
   *
   * <p>Executes asynchronously on a Virtual Thread to avoid blocking the main Servlet engine.
   *
   * @param conversationId The session identifier for thread isolation.
   * @param prompt The raw user message input.
   * @param user The currently authenticated user.
   * @param emitter The target {@link SseEmitter} connection.
   */
  public void streamSse(String conversationId, String prompt, User user, SseEmitter emitter) {
    logger.debug(
        "Starting SSE stream orchestration on virtual thread for conversation: {}", conversationId);

    virtualThreadExecutor.submit(
        () -> {
          try {
            String userId = user.id().toString();
            Set<String> roles = user.authorities();
            OrasakaContext context =
                new OrasakaContext(userId, conversationId, user.preferences(), roles);
            OrasakaChatRequest request = new OrasakaChatRequest(prompt, null, null, context);

            Flux<OrasakaChatResponse> stream = aiClient.stream(request);

            stream.subscribe(
                response -> {
                  try {
                    emitter.send(response);
                  } catch (Exception e) {
                    logger.error(
                        "Failed to send SSE chunk for conversation: {}", conversationId, e);
                  }
                },
                error -> {
                  logger.error(
                      "Error encountered in SSE reactive stream for conversation: {}",
                      conversationId,
                      error);
                  try {
                    emitter.completeWithError(error);
                  } catch (Exception ex) {
                  }
                },
                () -> {
                  logger.info(
                      "SSE stream completed successfully for conversation: {}", conversationId);
                  try {
                    emitter.complete();
                  } catch (Exception ex) {
                  }
                });
          } catch (Exception e) {
            logger.error(
                "Failed to initialize SSE stream execution for conversation: {}",
                conversationId,
                e);
            try {
              emitter.completeWithError(e);
            } catch (Exception ex) {
            }
          }
        });
  }

  /**
   * Resolves the reactive stream context and returns a {@link Flux} for GraphQL subscriptions.
   *
   * <p>Assembles context dynamically without artificial thread sleeps.
   *
   * @param conversationId The session identifier for thread isolation.
   * @param prompt The raw user message input.
   * @param user The currently authenticated user.
   * @return A reactive {@link Flux} emitting {@link OrasakaChatResponse} chunks.
   * @see OrasakaAiClient#stream(OrasakaChatRequest)
   */
  public Flux<OrasakaChatResponse> streamGraphQL(String conversationId, String prompt, User user) {
    logger.debug("Constructing GraphQL subscription stream for conversation: {}", conversationId);

    String userId = user.id().toString();
    Set<String> roles = user.authorities();
    OrasakaContext context = new OrasakaContext(userId, conversationId, user.preferences(), roles);
    OrasakaChatRequest request = new OrasakaChatRequest(prompt, null, null, context);

    return aiClient.stream(request);
  }
}
