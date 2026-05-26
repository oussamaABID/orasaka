package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.audio.AudioRequest;
import com.orasaka.core.domain.model.audio.AudioResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeechSynthesisStrategyTest {

  @TempDir Path tempDir;

  @Mock private AiClient aiClient;
  @Mock private CatalogModelManager catalogModelManager;

  private SpeechSynthesisStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new SpeechSynthesisStrategy(aiClient, catalogModelManager, tempDir.toString());
  }

  @Test
  void supports_withSpeechKey_returnsTrue() {
    assertTrue(strategy.supports("orasaka.core.media.speech.synthesis"));
    assertFalse(strategy.supports("orasaka.core.chat.text"));
    assertFalse(strategy.supports(null));
  }

  @Test
  void execute_successfulSynthesis_savesToFile() throws Exception {
    String jobId = "job-1";
    String userId = "user-1";

    Map<String, Object> payload = Map.of("prompt", "Hello world", "voice", "ryan");
    JobMessage message = new JobMessage(jobId, userId, "speech.synthesis", "", payload);
    Context context = Context.anonymous();

    byte[] audioBytes = new byte[] {5, 6, 7};
    AudioResponse response = new AudioResponse(audioBytes, "mp3");
    when(aiClient.audio(any(AudioRequest.class))).thenReturn(response);
    when(catalogModelManager.getDefaultModelByCategory("speech")).thenReturn(Optional.empty());

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertTrue(result.get("url").toString().contains("job-1/output/speech.mp3"));
    assertEquals("mp3", result.get("format"));
    assertNotNull(result.get("durationMs"));
  }

  @Test
  void execute_simulatedFailure_throwsJobExecutionException() {
    String jobId = "job-1";
    String userId = "user-1";

    Map<String, Object> payload = Map.of("prompt", "FAIL: this should fail");
    JobMessage message = new JobMessage(jobId, userId, "speech.synthesis", "", payload);
    Context context = Context.anonymous();

    assertThrows(JobExecutionException.class, () -> strategy.execute(message, context));
  }
}
