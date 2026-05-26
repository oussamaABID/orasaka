package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.application.processing.AudioPreProcessor;
import com.orasaka.core.domain.model.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Executes audio transcription/analysis jobs by delegating to the {@link AudioPreProcessor}.
 *
 * @since 1.1.0
 */
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
    String filePath = (String) message.payload().get("filePath");
    if (filePath == null) {
      throw new JobExecutionException("Payload does not contain filePath field");
    }
    File file = new File(filePath);
    if (!file.exists()) {
      throw new JobExecutionException("Audio file not found at " + filePath);
    }

    try {
      String model = (String) message.payload().get("model");
      byte[] audioBytes = Files.readAllBytes(file.toPath());
      var payload = audioPreProcessor.process(audioBytes, model);

      String responseContent = payload.transcript();
      if (responseContent == null) {
        responseContent = "";
      }

      Map<String, Object> result = new HashMap<>();
      result.put("analysis", responseContent);
      return result;
    } catch (IOException e) {
      throw new JobExecutionException("Failed to read audio file: " + e.getMessage(), e);
    }
  }
}
