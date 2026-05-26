package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.application.processing.VideoPreProcessor;
import com.orasaka.core.domain.model.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Executes video analysis jobs by delegating to the {@link VideoPreProcessor}.
 *
 * @since 1.1.0
 */
@Component
@org.springframework.core.annotation.Order(1)
final class VideoAnalysisStrategy implements JobExecutionStrategy {

  private final VideoPreProcessor videoPreProcessor;

  VideoAnalysisStrategy(VideoPreProcessor videoPreProcessor) {
    this.videoPreProcessor = videoPreProcessor;
  }

  @Override
  public boolean supports(String featureKey) {
    return "orasaka.core.media.video.analysis".equals(featureKey);
  }

  @Override
  public Map<String, Object> execute(JobMessage message, Context context)
      throws JobExecutionException {
    String filePath = (String) message.payload().get("filePath");
    if (filePath == null) {
      throw new JobExecutionException("Payload does not contain filePath field");
    }
    File file = new File(filePath);
    if (!file.exists()) {
      throw new JobExecutionException("Video file not found at " + filePath);
    }

    try {
      String model = (String) message.payload().get("model");
      byte[] videoBytes = Files.readAllBytes(file.toPath());
      var payload = videoPreProcessor.process(videoBytes, model);

      Map<String, Object> result = new HashMap<>();
      result.put("transcript", payload.audioTranscript());
      result.put("keyframeCount", payload.keyframes().size());
      return result;
    } catch (IOException e) {
      throw new JobExecutionException("Failed to read video file: " + e.getMessage(), e);
    }
  }
}
