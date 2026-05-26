package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.application.pipeline.DynamicPipelineExecutor;
import com.orasaka.core.domain.model.AdvancedPipelineSchema;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.gateway.application.processing.ContextResolver;
import com.orasaka.gateway.domain.model.ChatStreamRequest;
import com.orasaka.gateway.infrastructure.support.SseStreamHelper;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * REST controller exposing real-time Server-Sent Events (SSE) for chat streaming.
 *
 * <p>This controller focuses exclusively on the chat conversation streaming endpoints. Media
 * analysis has been extracted to {@link MediaAnalysisController} and code generation to {@link
 * CodeStreamController} per §2.7 protocol segregation.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatStreamController {

  private static final Logger logger = LoggerFactory.getLogger(ChatStreamController.class);

  private final AiClient aiClient;
  private final UserProfileProvider userProfileProvider;
  private final DynamicPipelineExecutor pipelineExecutor;
  private final int jobTimeoutSeconds;

  /**
   * Constructs the chat stream controller.
   *
   * @param aiClient the core AI facade
   * @param userProfileProvider the user profile provider
   * @param pipelineExecutor the two-phase pipeline executor for Early-Ack schema building
   * @param jobTimeoutSeconds the SSE stream timeout in seconds
   */
  public ChatStreamController(
      AiClient aiClient,
      UserProfileProvider userProfileProvider,
      DynamicPipelineExecutor pipelineExecutor,
      @Value("${orasaka.jobs.timeout-seconds:30}") int jobTimeoutSeconds) {
    this.aiClient = aiClient;
    this.userProfileProvider = userProfileProvider;
    this.pipelineExecutor = pipelineExecutor;
    this.jobTimeoutSeconds = jobTimeoutSeconds;
  }

  /**
   * Streams chat via SSE using GET.
   *
   * @param conversationId the conversation ID
   * @param prompt the prompt text
   * @param user the authenticated user principal
   * @return SseEmitter instance
   */
  @GetMapping(value = "/stream/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamChat(
      @PathVariable String conversationId,
      @RequestParam String prompt,
      @AuthenticationPrincipal User user) {
    logger.debug("SSE GET stream: conversation={}, prompt={}", conversationId, prompt);
    return streamSse(
        conversationId, new ChatRequest(prompt, null, null, resolveContext(user, conversationId)));
  }

  /**
   * Streams chat via SSE using POST (supporting file reference mappings).
   *
   * @param conversationId the conversation ID
   * @param requestPayload the structured request body payload
   * @param user the authenticated user principal
   * @return SseEmitter instance
   */
  @PostMapping(
      value = "/stream/{conversationId}",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public SseEmitter streamChatPost(
      @PathVariable String conversationId,
      @RequestBody ChatStreamRequest requestPayload,
      @AuthenticationPrincipal User user) {
    logger.debug(
        "SSE POST stream: conversation={}, prompt={}, assetIds={}",
        conversationId,
        requestPayload.prompt(),
        requestPayload.assetIds());
    Map<String, Object> settings = new HashMap<>();
    settings.put("assetIds", requestPayload.assetIds());
    if (requestPayload.model() != null) {
      settings.put("model", requestPayload.model());
    }
    if (requestPayload.videoSteps() != null) {
      settings.put("videoSteps", requestPayload.videoSteps());
    }
    if (requestPayload.videoFps() != null) {
      settings.put("videoFps", requestPayload.videoFps());
    }
    if (requestPayload.motionBucketId() != null) {
      settings.put("motionBucketId", requestPayload.motionBucketId());
    }
    settings.put("pipelineId", requestPayload.pipelineId());
    return streamSse(
        conversationId,
        new ChatRequest(
            requestPayload.prompt(), null, settings, resolveContext(user, conversationId)),
        requestPayload.pipelineId());
  }

  private Context resolveContext(User user, String conversationId) {
    return ContextResolver.resolve(user, conversationId, userProfileProvider);
  }

  private SseEmitter streamSse(String conversationId, ChatRequest request) {
    return streamSse(conversationId, request, "default");
  }

  private SseEmitter streamSse(String conversationId, ChatRequest request, String pipelineId) {
    logger.debug("SSE stream starting: conversation={}, pipeline={}", conversationId, pipelineId);
    SseEmitter emitter = SseStreamHelper.createEmitter();

    try {
      // ── Early-Ack: push pipeline architecture metadata before LLM inference ──
      emitPipelineAck(emitter, conversationId, pipelineId, request.prompt());

      Flux<ChatResponse> stream =
          aiClient.stream(request).timeout(Duration.ofSeconds(jobTimeoutSeconds));

      logger.debug("Subscribing to flux stream for conversation {}", conversationId);
      stream.subscribe(
          response -> {
            String content = response.content();
            if (content != null && !content.isEmpty()) {
              try {
                emitter.send(
                    SseEmitter.event()
                        .id(conversationId)
                        .name("message")
                        .data(Map.of("content", content), MediaType.APPLICATION_JSON));
              } catch (IOException ioException) {
                logger.debug("Client disconnected from SSE stream [{}] mid-stream", conversationId);
              }
            }
          },
          error -> {
            logger.error("Failed to initialize SSE stream [{}]", conversationId, error);
            try {
              emitter.completeWithError(error);
            } catch (Exception ignored) {
              logger.trace("Emitter already closed during error completion [{}]", conversationId);
            }
          },
          () -> {
            logger.debug("SSE stream completed [{}]", conversationId);
            try {
              emitter.complete();
            } catch (Exception ignored) {
              logger.trace("Emitter already closed during normal completion [{}]", conversationId);
            }
          });
    } catch (Exception e) {
      logger.error(
          "Exception thrown during streamSse setup for conversation {}", conversationId, e);
      try {
        emitter.completeWithError(e);
      } catch (Exception ignored) {
        logger.trace("Emitter already closed during setup error [{}]", conversationId);
      }
    }

    return emitter;
  }

  /**
   * Emits the Early-Ack SSE event containing the compiled pipeline architecture schema.
   *
   * <p>This event fires before the LLM begins generating tokens, providing the UI with immediate
   * visibility into the active interceptor chain composition and estimated latency overhead.
   *
   * @param emitter The SSE emitter to send through.
   * @param conversationId The conversation trace ID.
   * @param pipelineId The pipeline configuration identifier.
   * @param prompt The raw prompt for schema building.
   */
  private void emitPipelineAck(
      SseEmitter emitter, String conversationId, String pipelineId, String prompt) {
    try {
      AdvancedPipelineSchema schema = pipelineExecutor.buildSchema(pipelineId, prompt);
      emitter.send(
          SseEmitter.event()
              .id(conversationId)
              .name("pipeline-ack")
              .data(schema, MediaType.APPLICATION_JSON));
      logger.debug(
          "Early-Ack emitted: pipeline={}, interceptors={}",
          schema.pipelineId(),
          schema.totalInterceptorCount());
    } catch (IOException e) {
      logger.debug("Client disconnected before Early-Ack could be sent [{}]", conversationId);
    } catch (Exception e) {
      logger.warn("Failed to build/send Early-Ack pipeline schema — proceeding without it.", e);
    }
  }
}
