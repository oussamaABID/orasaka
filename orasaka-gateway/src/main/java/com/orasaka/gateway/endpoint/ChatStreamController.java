package com.orasaka.gateway.endpoint;

import com.orasaka.core.engine.OrasakaAiClient;
import com.orasaka.core.support.OrasakaChatRequest;
import com.orasaka.core.support.OrasakaContext;
import com.orasaka.core.support.OrasakaImageRequest;
import com.orasaka.core.support.OrasakaSpeechRequest;
import com.orasaka.gateway.service.ChatStreamService;
import com.orasaka.gateway.support.MediaContracts;
import com.orasaka.identity.domain.User;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller exposing real-time Server-Sent Events (SSE) chat streaming and media analysis.
 */
@RestController
public class ChatStreamController {

  private static final Logger logger = LoggerFactory.getLogger(ChatStreamController.class);
  private static final String CHAT_STREAM_PATH = "/api/v1/chat/stream/{conversationId}";

  private final ChatStreamService chatStreamService;
  private final OrasakaAiClient aiClient;

  /**
   * Constructs the controller.
   *
   * @param chatStreamService The chat stream service.
   * @param aiClient The core AI facade.
   */
  public ChatStreamController(ChatStreamService chatStreamService, OrasakaAiClient aiClient) {
    this.chatStreamService = chatStreamService;
    this.aiClient = aiClient;
  }

  private User getCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User user) {
      return user;
    }
    throw new AccessDeniedException("Access Denied: User is not authenticated");
  }

  /**
   * Streams chat via SSE.
   *
   * @param conversationId The conversation ID.
   * @param prompt The prompt.
   * @return SseEmitter instance.
   */
  @GetMapping(value = CHAT_STREAM_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamChat(@PathVariable String conversationId, @RequestParam String prompt) {
    logger.debug(
        "SSE stream request received for conversationId: {}, prompt: {}", conversationId, prompt);
    SseEmitter emitter = new SseEmitter(0L);
    User user = getCurrentUser();
    chatStreamService.streamSse(conversationId, prompt, user, emitter);
    return emitter;
  }

  /**
   * Generate image REST endpoint.
   *
   * @param body The prompt wrapper.
   * @return Response containing image url.
   */
  @PostMapping("/api/v1/chat/image")
  public ResponseEntity<?> generateImage(@RequestBody Map<String, String> body) {
    String prompt = body.get("prompt");
    if (prompt == null || prompt.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
    }
    User user = getCurrentUser();
    var context =
        new OrasakaContext(user.id().toString(), null, user.preferences(), user.authorities());
    var request = new OrasakaImageRequest(prompt, null, null, null, context);
    var response = aiClient.generateImage(request);
    return ResponseEntity.ok(Map.of("content", response.url()));
  }

  /**
   * Generate speech REST endpoint.
   *
   * @param body The text wrapper.
   * @return Response containing speech audio url.
   */
  @PostMapping("/api/v1/chat/speech")
  public ResponseEntity<?> generateSpeech(@RequestBody Map<String, String> body) {
    String text = body.get("text");
    if (text == null || text.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
    }
    User user = getCurrentUser();
    var context =
        new OrasakaContext(user.id().toString(), null, user.preferences(), user.authorities());
    var request = new OrasakaSpeechRequest(text, null, context);
    byte[] audioBytes = aiClient.generateSpeech(request);
    String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
    String audioUrl = "data:audio/mp3;base64," + base64Audio;
    return ResponseEntity.ok(Map.of("content", audioUrl));
  }

  /**
   * Analyze image REST endpoint.
   *
   * @param request The poster analysis request.
   * @return Response containing analysis content.
   */
  @PostMapping("/api/v1/media/analyze-image")
  public ResponseEntity<?> analyzePoster(@RequestBody MediaContracts.AnalyzePosterRequest request) {
    User user = getCurrentUser();
    var context =
        new OrasakaContext(user.id().toString(), null, user.preferences(), user.authorities());
    var chatRequest =
        new OrasakaChatRequest(
            request.prompt() != null ? request.prompt() : "Analyze this poster",
            null,
            null,
            context);
    var response = aiClient.chat(chatRequest);
    return ResponseEntity.ok(Map.of("analysis", response.content()));
  }

  /**
   * Analyze audio REST endpoint.
   *
   * @param request The audio analysis request.
   * @return Response containing compliance analysis content.
   */
  @PostMapping("/api/v1/media/analyze-audio")
  public ResponseEntity<?> analyzeAudio(@RequestBody MediaContracts.AnalyzeAudioRequest request) {
    User user = getCurrentUser();
    var context =
        new OrasakaContext(user.id().toString(), null, user.preferences(), user.authorities());
    var chatRequest =
        new OrasakaChatRequest(
            "Analyze compliance for audio clip in thread: "
                + request.threadId()
                + " with audio base64 length: "
                + request.audioBase64().length(),
            null,
            null,
            context);
    var response = aiClient.chat(chatRequest);
    return ResponseEntity.ok(Map.of("analysis", response.content()));
  }
}
