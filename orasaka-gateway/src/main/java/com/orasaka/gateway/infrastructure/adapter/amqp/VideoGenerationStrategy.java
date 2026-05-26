package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.video.VideoRequest;
import com.orasaka.core.domain.model.video.VideoResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.gateway.infrastructure.support.PathResolver;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Executes video generation jobs via the AI client's video endpoint.
 *
 * @since 1.1.0
 */
@Component
@org.springframework.core.annotation.Order(1)
final class VideoGenerationStrategy implements JobExecutionStrategy {

  private static final Logger logger = LoggerFactory.getLogger(VideoGenerationStrategy.class);

  private final AiClient aiClient;
  private final CatalogModelManager catalogModelManager;
  private final String uploadDir;

  VideoGenerationStrategy(
      AiClient aiClient,
      CatalogModelManager catalogModelManager,
      @Value("${spring.servlet.multipart.location:var/orasaka-uploads}") String uploadDir) {
    this.aiClient = aiClient;
    this.catalogModelManager = catalogModelManager;
    this.uploadDir = PathResolver.resolveToString(uploadDir);
  }

  @Override
  public boolean supports(String featureKey) {
    return featureKey != null && featureKey.contains("video") && !featureKey.contains("analysis");
  }

  @Override
  public Map<String, Object> execute(JobMessage message, Context context)
      throws JobExecutionException {
    String prompt = JobStrategyHelper.extractPrompt(message);

    Integer duration = (Integer) message.payload().get("durationSeconds");
    if (duration == null) {
      duration = 5;
    }

    String inputPath = resolveInputPath(message);
    String outputPath = prepareOutputPath(message);
    String model =
        JobStrategyHelper.resolveModel(
            message, "video", "stable-video-diffusion-img2vid-xt", catalogModelManager);

    Map<String, Object> settings = new HashMap<>();
    settings.put("jobId", message.jobId());
    if (!inputPath.isEmpty()) {
      settings.put("image_path", inputPath);
    }
    if (!outputPath.isEmpty()) {
      settings.put("output_path", outputPath);
    }

    VideoRequest videoRequest =
        new VideoRequest(
            prompt, duration, model, message.jobId(), inputPath, outputPath, settings, context);
    VideoResponse videoResponse = aiClient.video(videoRequest);

    byte[] videoData = videoResponse.videoData();
    String translatedUrl =
        JobMediaHelper.saveMediaToFile(
            uploadDir, message.userId(), message.jobId(), videoData, "video.mp4");

    Map<String, Object> result = new HashMap<>();
    result.put("url", translatedUrl);
    result.put("format", "mp4");
    if (videoResponse.metrics() != null && !videoResponse.metrics().isEmpty()) {
      result.put("metrics", videoResponse.metrics());
    }
    return result;
  }

  private String resolveInputPath(JobMessage message) {
    String imagePath = (String) message.payload().get("imagePath");
    if (imagePath != null && !imagePath.isBlank()) {
      File file = new File(imagePath);
      if (file.exists()) {
        return file.getAbsolutePath();
      }
    }
    return "";
  }

  private String prepareOutputPath(JobMessage message) {
    try {
      Path dir =
          Paths.get(uploadDir, message.userId(), message.jobId(), "output")
              .toAbsolutePath()
              .normalize();
      Files.createDirectories(dir);
      return dir.resolve("video.mp4").toAbsolutePath().toString();
    } catch (IOException ex) {
      logger.error("Failed to prepare zero-copy output directory", ex);
      return "";
    }
  }
}
