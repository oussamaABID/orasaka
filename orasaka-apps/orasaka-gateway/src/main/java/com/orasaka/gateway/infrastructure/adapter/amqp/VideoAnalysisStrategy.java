package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.application.processing.VideoPreProcessor;
import com.orasaka.core.domain.model.Context;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Executes video analysis jobs by delegating to the {@link VideoPreProcessor}. */
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
    byte[] videoBytes = extractFileBytes(message, "Video");
    String model = (String) message.payload().get("model");
    var payload = videoPreProcessor.process(videoBytes, model);

    Map<String, Object> result = new HashMap<>();
    result.put("transcript", payload.audioTranscript());
    result.put("keyframeCount", payload.keyframes().size());
    return result;
  }
}
