package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.gateway.application.processing.ContextResolver;
import com.orasaka.gateway.domain.model.CodeGenerationRequest;
import com.orasaka.gateway.infrastructure.support.SseStreamHelper;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * REST controller for code generation SSE streaming.
 *
 * <p>Extracted from {@code ChatStreamController} to enforce single-responsibility (§2.7 protocol
 * segregation). Uses {@link SseStreamHelper} for SSE lifecycle management.
 *
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/v1/chat")
public class CodeStreamController {

  private static final Logger logger = LoggerFactory.getLogger(CodeStreamController.class);
  private static final String DEFAULT_CODE_MODEL = "qwen2.5-coder:7b";

  private final AiClient aiClient;
  private final UserProfileProvider userProfileProvider;
  private final ExecutorService virtualThreadExecutor;
  private final int jobTimeoutSeconds;

  public CodeStreamController(
      AiClient aiClient,
      UserProfileProvider userProfileProvider,
      ExecutorService virtualThreadExecutor,
      @Value("${orasaka.jobs.timeout-seconds:30}") int jobTimeoutSeconds) {
    this.aiClient = aiClient;
    this.userProfileProvider = userProfileProvider;
    this.virtualThreadExecutor = virtualThreadExecutor;
    this.jobTimeoutSeconds = jobTimeoutSeconds;
  }

  /**
   * Streams code generation via SSE using POST.
   *
   * @param requestPayload the structured code generation payload
   * @param user the authenticated user principal
   * @return SseEmitter instance
   */
  @PostMapping(
      value = "/code",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public SseEmitter streamCode(
      @RequestBody CodeGenerationRequest requestPayload, @AuthenticationPrincipal User user) {
    logger.debug(
        "SSE code stream request: prompt={}, model={}",
        requestPayload.prompt(),
        requestPayload.model());

    SseEmitter emitter = SseStreamHelper.createEmitter();

    virtualThreadExecutor.submit(
        () -> {
          try {
            Context context = ContextResolver.resolve(user, null, userProfileProvider);
            String model =
                (requestPayload.model() != null && !requestPayload.model().isBlank())
                    ? requestPayload.model()
                    : DEFAULT_CODE_MODEL;
            Map<String, Object> settings = Map.of("provider", "ollama", "model", model);
            ChatRequest request = new ChatRequest(requestPayload.prompt(), null, settings, context);
            Flux<ChatResponse> stream =
                aiClient.stream(request).timeout(Duration.ofSeconds(jobTimeoutSeconds));
            SseStreamHelper.subscribe(emitter, stream, "code-gen");
          } catch (Exception e) {
            logger.error("Failed to initialize SSE code stream", e);
            try {
              emitter.completeWithError(e);
            } catch (Exception ignored) {
              // Emitter already closed
            }
          }
        });

    return emitter;
  }
}
