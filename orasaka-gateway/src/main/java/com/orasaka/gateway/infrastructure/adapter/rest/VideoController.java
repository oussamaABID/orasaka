package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.gateway.infrastructure.support.PathResolver;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for executing video generation tasks. */
@RestController
@RequestMapping("/api/v1")
public class VideoController {

  private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

  private final JobService jobService;
  private final JobQueuePublisherService jobQueuePublisher;
  private final String uploadDirProperty;

  /**
   * Constructs the video controller.
   *
   * @param jobService Job service from core.
   * @param jobQueuePublisher Job queue publisher.
   * @param uploadDirProperty Upload directory path.
   */
  public VideoController(
      JobService jobService,
      JobQueuePublisherService jobQueuePublisher,
      @Value("${spring.servlet.multipart.location:var/orasaka-uploads}") String uploadDirProperty) {
    this.jobService = Objects.requireNonNull(jobService, "JobService must not be null");
    this.jobQueuePublisher =
        Objects.requireNonNull(jobQueuePublisher, "JobQueuePublisherService must not be null");
    this.uploadDirProperty = PathResolver.resolveToString(uploadDirProperty);
  }

  /**
   * REST endpoint to generate a video from a prompt.
   *
   * @param request The video generation payload request parameters.
   * @param user The authenticated user principal.
   * @return Safe HTTP response containing the Job ID.
   */
  @PostMapping("/ai/video")
  public ResponseEntity<Map<String, Object>> generateVideo(
      @RequestBody VideoGenerationRequest request, @AuthenticationPrincipal User user) {
    String activeModel = resolveModel(request.model());
    String userId = user.id().toString();
    String jobId = UUID.randomUUID().toString();

    initJobDirectories(userId, jobId);

    Map<String, Object> payload = buildPayload(request, activeModel);
    String targetFilePath = resolveImageAsset(request.image(), userId, jobId);

    if (request.image() != null && !request.image().isBlank()) {
      payload.put("image", request.image());
    }
    if (targetFilePath != null) {
      payload.put("imagePath", targetFilePath);
      try {
        Path pathObj = Paths.get(targetFilePath);
        String mimeType = Files.probeContentType(pathObj);
        if (mimeType == null) {
          mimeType = "image/png";
        }
        payload.put("mimeType", mimeType);
      } catch (Exception e) {
        payload.put("mimeType", "image/png");
      }
    }

    jobService.createJob(jobId, userId, "orasaka.core.media.video", payload);

    JobMessage message =
        new JobMessage(jobId, userId, "orasaka.core.media.video", activeModel, payload);
    jobQueuePublisher.publish(message);

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(Map.of("jobId", jobId, "status", "PENDING"));
  }

  private String resolveModel(String model) {
    if (model == null || model.isBlank()) {
      return "stable-video-diffusion-img2vid-xt";
    }
    Set<String> workerRegistry =
        Set.of(
            "stable-video-diffusion-img2vid-xt",
            "animatediff-lightning-mps",
            "apple-coreml-video-pipeline",
            "mlx-animatediff-lightning",
            "mlx-stable-diffusion-video",
            "stable-video-diffusion-img2vid-xt-mps-fp32",
            "argmaxinc/mlx-FLUX.1-schnell-4bit-quantized",
            "ByteDance/AnimateDiff-Lightning");
    if (!workerRegistry.contains(model)) {
      throw new InvalidRequestException(
          "Model '" + model + "' is not registered in the video worker catalog registry.");
    }
    return model;
  }

  private void initJobDirectories(String userId, String jobId) {
    try {
      Files.createDirectories(Paths.get(uploadDirProperty, userId, jobId, "input"));
      Files.createDirectories(Paths.get(uploadDirProperty, userId, jobId, "output"));
      Files.createDirectories(Paths.get(uploadDirProperty, userId, jobId, "temp"));
    } catch (IOException e) {
      logger.error("Failed to initialize job directories", e);
    }
  }

  private Map<String, Object> buildPayload(VideoGenerationRequest request, String activeModel) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("prompt", request.prompt());
    payload.put(
        "durationSeconds", request.durationSeconds() != null ? request.durationSeconds() : 5);
    payload.put("model", activeModel);
    if (request.numFrames() != null) {
      payload.put("num_frames", request.numFrames());
    }
    if (request.videoFps() != null) {
      payload.put("video_fps", request.videoFps());
    }
    return payload;
  }

  private String resolveImageAsset(String imageAssetId, String userId, String jobId) {
    if (imageAssetId == null || imageAssetId.isBlank()) {
      return null;
    }
    try {
      File sourceFile = findImageFile(imageAssetId, userId);
      if (sourceFile != null) {
        Path targetPath =
            Paths.get(uploadDirProperty, userId, jobId, "input").resolve(sourceFile.getName());
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toAbsolutePath().toString();
      }
    } catch (IOException e) {
      logger.error("Failed to copy image for video generation", e);
    }
    return null;
  }

  private File findImageFile(String assetId, String userId) {
    File userDir = new File(uploadDirProperty, userId);
    File tempDir = new File(userDir, "temp");

    if (tempDir.exists()) {
      File[] matches = tempDir.listFiles((dir, name) -> name.startsWith(assetId));
      if (matches != null && matches.length > 0) {
        return matches[0];
      }
    }
    if (userDir.exists()) {
      File[] matches = userDir.listFiles((dir, name) -> name.startsWith(assetId));
      if (matches != null && matches.length > 0) {
        return matches[0];
      }
    }
    return null;
  }
}
