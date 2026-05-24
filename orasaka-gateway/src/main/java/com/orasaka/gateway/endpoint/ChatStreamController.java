package com.orasaka.gateway.endpoint;

import com.orasaka.core.client.OrasakaAiClient;
import com.orasaka.core.infrastructure.video.OrasakaVideoService;
import com.orasaka.core.ingest.video.OrasakaVideoRequest;
import com.orasaka.core.ingest.video.OrasakaVideoResponse;
import com.orasaka.core.support.OrasakaAuthority;
import com.orasaka.core.support.OrasakaChatRequest;
import com.orasaka.core.support.OrasakaContext;
import com.orasaka.core.support.OrasakaImageRequest;
import com.orasaka.core.support.OrasakaSpeechRequest;
import com.orasaka.gateway.service.ChatStreamService;
import com.orasaka.gateway.support.MediaContracts;
import com.orasaka.identity.domain.User;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
@RequestMapping("/api/v1")
public class ChatStreamController {

  private static final Logger logger = LoggerFactory.getLogger(ChatStreamController.class);

  private final ChatStreamService chatStreamService;
  private final OrasakaAiClient aiClient;
  private final OrasakaVideoService orasakaVideoService;

  /**
   * Constructs the controller.
   *
   * @param chatStreamService The chat stream service.
   * @param aiClient The core AI facade.
   * @param orasakaVideoService The video generation service.
   */
  public ChatStreamController(
      ChatStreamService chatStreamService,
      OrasakaAiClient aiClient,
      OrasakaVideoService orasakaVideoService) {
    this.chatStreamService = chatStreamService;
    this.aiClient = aiClient;
    this.orasakaVideoService = orasakaVideoService;
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
  @GetMapping(value = "/chat/stream/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
  @PostMapping("/chat/image")
  public ResponseEntity<?> generateImage(@RequestBody Map<String, String> body) {
    String prompt = body.get("prompt");
    if (prompt == null || prompt.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
    }
    User user = getCurrentUser();
    OrasakaContext context = buildContext(user);
    var request = new OrasakaImageRequest(prompt, null, null, null, context);
    var response = aiClient.generateImage(request);

    // Elite Defensive Payload Packing
    String base64Data = "";
    if (response.imageData() != null) {
      base64Data = Base64.getEncoder().encodeToString(response.imageData());
    }

    Map<String, Object> safeResponse = new HashMap<>();
    safeResponse.put("format", response.format() != null ? response.format() : "png");

    String urlVal = "";
    if (base64Data != null && !base64Data.isBlank()) {
      urlVal = "data:image/png;base64," + base64Data.trim();
    } else if (response.url() != null) {
      urlVal = response.url();
    }

    safeResponse.put("url", urlVal);
    safeResponse.put("content", urlVal);

    return ResponseEntity.ok(safeResponse);
  }

  /**
   * Generate speech REST endpoint.
   *
   * @param body The text wrapper.
   * @return Response containing speech audio url.
   */
  @PostMapping("/chat/speech")
  public ResponseEntity<?> generateSpeech(@RequestBody Map<String, String> body) {
    String text = body.get("text");
    if (text == null || text.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
    }
    User user = getCurrentUser();
    OrasakaContext context = buildContext(user);
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
  @PostMapping("/media/analyze-image")
  public ResponseEntity<?> analyzePoster(@RequestBody MediaContracts.AnalyzePosterRequest request) {
    User user = getCurrentUser();
    OrasakaContext context = buildContext(user);
    var chatRequest =
        new OrasakaChatRequest(
            (request.prompt() != null ? request.prompt() : "Analyze this poster")
                + " [posterBase64: "
                + request.posterBase64()
                + "]",
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
  @PostMapping("/media/analyze-audio")
  public ResponseEntity<?> analyzeAudio(@RequestBody MediaContracts.AnalyzeAudioRequest request) {
    User user = getCurrentUser();
    OrasakaContext context = buildContext(user);
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

  /**
   * Text-to-Video generation endpoint.
   *
   * @param request The video generation request parameters.
   * @return Base64 data URL representing the video content.
   */
  @PostMapping("/ai/video")
  public ResponseEntity<Map<String, Object>> generateVideo(
      @RequestBody OrasakaVideoRequest request) {
    User user = getCurrentUser();
    OrasakaContext context = buildContext(user);
    var secureRequest =
        new OrasakaVideoRequest(
            request.prompt(), request.durationSeconds(), request.settings(), context);

    OrasakaVideoResponse response = orasakaVideoService.generateVideo(secureRequest);
    String b64Data = Base64.getEncoder().encodeToString(response.videoData());

    return ResponseEntity.ok(Map.of("format", "mp4", "url", "data:video/mp4;base64," + b64Data));
  }

  private OrasakaContext buildContext(User user) {
    Set<OrasakaAuthority> authorities =
        user.authorities().stream().map(OrasakaAuthority::new).collect(Collectors.toSet());
    return new OrasakaContext(user.id().toString(), null, user.preferences(), authorities);
  }
}
