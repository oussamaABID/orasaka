package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.ports.outbound.KnowledgeService;
import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.domain.model.AudioAnalysisRequest;
import com.orasaka.gateway.domain.model.MediaContracts;
import com.orasaka.gateway.domain.model.VideoAnalysisRequest;
import com.orasaka.gateway.infrastructure.support.AssetFileResolver;
import com.orasaka.gateway.infrastructure.support.JobSubmissionHelper;
import com.orasaka.gateway.infrastructure.support.PathResolver;
import com.orasaka.identity.domain.model.User;
import com.orasaka.persistence.domain.ports.inbound.JobPersistenceProvider;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for media analysis endpoints: image, audio, video analysis and RAG search.
 *
 * <p>Extracted from {@code ChatStreamController} to enforce single-responsibility (§2.7 protocol
 * segregation). Each endpoint delegates file resolution and job submission to shared utilities.
 *
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaAnalysisController {

  private static final Logger logger = LoggerFactory.getLogger(MediaAnalysisController.class);
  private static final String MODEL_KEY = "model";
  private final KnowledgeService knowledgeService;
  private final JobPersistenceProvider jobPersistenceProvider;
  private final JobQueuePublisherService jobQueuePublisher;
  private final String uploadDir;

  public MediaAnalysisController(
      KnowledgeService knowledgeService,
      JobPersistenceProvider jobPersistenceProvider,
      JobQueuePublisherService jobQueuePublisher,
      @Value("${spring.servlet.multipart.location:var/orasaka-uploads}") String uploadDirProperty) {
    this.knowledgeService = knowledgeService;
    this.jobPersistenceProvider = jobPersistenceProvider;
    this.jobQueuePublisher = jobQueuePublisher;
    this.uploadDir = PathResolver.resolveToString(uploadDirProperty);
  }

  /**
   * Performs a passive RAG context search against the semantic index.
   *
   * @param query the search query
   * @return formatted context string JSON
   */
  @GetMapping("/search-rag")
  public ResponseEntity<Map<String, Object>> searchRag(@RequestParam("q") String query) {
    String context = knowledgeService.retrieveContext(query, 5);
    return ResponseEntity.ok(Map.of("context", context));
  }

  /**
   * Submits an image analysis job.
   *
   * @param request the poster analysis request
   * @param user the authenticated user principal
   * @return response containing the Job ID
   */
  @PostMapping("/analyze-image")
  public ResponseEntity<Object> analyzeImage(
      @RequestBody MediaContracts.AnalyzePosterRequest request,
      @AuthenticationPrincipal User user) {
    return submitMediaJob(
        user.id().toString(),
        request.assetId(),
        "orasaka.core.media.vision",
        buildImagePayload(request));
  }

  /**
   * Submits an audio analysis job.
   *
   * @param request the audio analysis request
   * @param user the authenticated user principal
   * @return response containing the Job ID
   */
  @PostMapping("/analyze-audio")
  public ResponseEntity<Object> analyzeAudio(
      @RequestBody AudioAnalysisRequest request, @AuthenticationPrincipal User user) {
    return submitMediaJob(
        user.id().toString(),
        request.assetId(),
        "orasaka.core.media.audio",
        buildAudioPayload(request));
  }

  /**
   * Submits a video analysis job.
   *
   * @param request the video analysis request
   * @param user the authenticated user principal
   * @return response containing the Job ID
   */
  @PostMapping("/analyze-video")
  public ResponseEntity<Object> analyzeVideo(
      @RequestBody VideoAnalysisRequest request, @AuthenticationPrincipal User user) {
    return submitMediaJob(
        user.id().toString(),
        request.assetId(),
        "orasaka.core.media.video.analysis",
        buildVideoPayload(request));
  }

  // ── Private helpers ──────────────────────────────────────────────────────

  private ResponseEntity<Object> submitMediaJob(
      String userId, Object assetId, String featureKey, Map<String, Object> payload) {
    Optional<File> sourceFile = AssetFileResolver.resolve(uploadDir, userId, assetId);
    if (sourceFile.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Asset file not found for ID: " + assetId));
    }
    try {
      var result =
          JobSubmissionHelper.submit(
              jobPersistenceProvider,
              jobQueuePublisher,
              uploadDir,
              userId,
              featureKey,
              payload,
              sourceFile.get());
      return ResponseEntity.status(HttpStatus.ACCEPTED)
          .body(Map.of("jobId", result.jobId(), "status", "PENDING"));
    } catch (IOException | RuntimeException e) {
      logger.error("Failed to submit {} job", featureKey, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to process file: " + e.getMessage()));
    }
  }

  private static Map<String, Object> buildImagePayload(
      MediaContracts.AnalyzePosterRequest request) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("prompt", request.prompt() != null ? request.prompt() : "Analyze this poster");
    if (request.model() != null && !request.model().isBlank()) {
      payload.put(MODEL_KEY, request.model());
    }
    return payload;
  }

  private static Map<String, Object> buildAudioPayload(AudioAnalysisRequest request) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("threadId", request.threadId() != null ? request.threadId() : "");
    if (request.model() != null && !request.model().isBlank()) {
      payload.put(MODEL_KEY, request.model());
    }
    return payload;
  }

  private static Map<String, Object> buildVideoPayload(VideoAnalysisRequest request) {
    Map<String, Object> payload = new HashMap<>();
    if (request.model() != null && !request.model().isBlank()) {
      payload.put(MODEL_KEY, request.model());
    }
    return payload;
  }
}
