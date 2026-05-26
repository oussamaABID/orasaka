package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.application.processing.AudioPreProcessor;
import com.orasaka.core.domain.model.Context;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Executes audio transcription/analysis jobs by delegating to the {@link AudioPreProcessor}. */
@Component
@org.springframework.core.annotation.Order(1)
final class AudioAnalysisStrategy implements JobExecutionStrategy {

  private final AudioPreProcessor audioPreProcessor;

  AudioAnalysisStrategy(AudioPreProcessor audioPreProcessor) {
    this.audioPreProcessor = audioPreProcessor;
  }

  @Override
  public boolean supports(String featureKey) {
    return featureKey != null && featureKey.contains("audio");
  }

  @Override
  public Map<String, Object> execute(JobMessage message, Context context)
      throws JobExecutionException {
    byte[] audioBytes = extractFileBytes(message, "Audio");
    String model = (String) message.payload().get("model");
    var payload = audioPreProcessor.process(audioBytes, model);

    String responseContent = payload.transcript();
    if (responseContent == null) {
      responseContent = "";
    }

    Map<String, Object> result = new HashMap<>();
    result.put("analysis", responseContent);
    return result;
  }
}
