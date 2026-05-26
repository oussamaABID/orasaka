package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.audio.AudioRequest;
import com.orasaka.core.domain.model.audio.AudioResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Executes text-to-speech synthesis jobs via the AI client's audio endpoint. */
@Component
@org.springframework.core.annotation.Order(1)
final class SpeechSynthesisStrategy implements JobExecutionStrategy {

  private final AiClient aiClient;
  private final CatalogModelManager catalogModelManager;
  private final String uploadDir;

  SpeechSynthesisStrategy(
      AiClient aiClient,
      CatalogModelManager catalogModelManager,
      @Value("${spring.servlet.multipart.location:var/orasaka-uploads}") String uploadDir) {
    this.aiClient = aiClient;
    this.catalogModelManager = catalogModelManager;
    this.uploadDir = uploadDir;
  }

  @Override
  public boolean supports(String featureKey) {
    return featureKey != null && featureKey.contains("speech");
  }

  @Override
  public Map<String, Object> execute(JobMessage message, Context context)
      throws JobExecutionException {
    String prompt = JobStrategyHelper.extractPrompt(message);

    // Simulation trigger for testing DLQ
    if (prompt.startsWith("FAIL")) {
      throw new JobExecutionException("Simulated speech synthesis failure for testing DLQ");
    }
    String model =
        JobStrategyHelper.resolveModel(
            message, "speech", "piper-en-medium-ryan", catalogModelManager);
    String voice = (String) message.payload().get("voice");

    AudioRequest audioRequest = new AudioRequest(prompt, voice, model, Map.of(), context);
    long startTime = System.currentTimeMillis();
    AudioResponse audioResponse = aiClient.audio(audioRequest);
    long durationMs = System.currentTimeMillis() - startTime;

    byte[] audioBytes = audioResponse.audioData();
    String audioUrl =
        JobMediaHelper.saveMediaToFile(
            uploadDir, message.userId(), message.jobId(), audioBytes, "speech.mp3");

    Map<String, Object> result = new HashMap<>();
    result.put("url", audioUrl);
    result.put("format", "mp3");
    result.put("durationMs", durationMs);
    return result;
  }
}
