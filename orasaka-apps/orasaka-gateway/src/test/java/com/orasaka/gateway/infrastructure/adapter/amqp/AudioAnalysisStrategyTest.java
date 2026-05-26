package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.application.processing.AudioPreProcessor;
import com.orasaka.core.application.processing.ProcessedAudioPayload;
import com.orasaka.core.domain.model.Context;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AudioAnalysisStrategyTest {

  @TempDir Path tempDir;

  @Mock private AudioPreProcessor audioPreProcessor;

  private AudioAnalysisStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new AudioAnalysisStrategy(audioPreProcessor);
  }

  @Test
  void supports_withAudioKey_returnsTrue() {
    assertTrue(strategy.supports("orasaka.core.media.audio.analysis"));
    assertFalse(strategy.supports("orasaka.core.chat.text"));
    assertFalse(strategy.supports(null));
  }

  @Test
  void execute_missingFilePath_throwsException() {
    JobMessage message = new JobMessage("job-1", "user-1", "audio", Map.of());
    Context context = Context.anonymous();

    assertThrows(JobExecutionException.class, () -> strategy.execute(message, context));
  }

  @Test
  void execute_fileNotFound_throwsException() {
    JobMessage message =
        new JobMessage("job-1", "user-1", "audio", Map.of("filePath", "missing.wav"));
    Context context = Context.anonymous();

    assertThrows(JobExecutionException.class, () -> strategy.execute(message, context));
  }

  @Test
  void execute_successfulAnalysis_returnsTranscript() throws Exception {
    Path audioFile = tempDir.resolve("test.wav");
    Files.write(audioFile, new byte[] {1, 2, 3});

    JobMessage message =
        new JobMessage(
            "job-1",
            "user-1",
            "audio",
            Map.of("filePath", audioFile.toAbsolutePath().toString(), "model", "whisper"));
    Context context = Context.anonymous();

    ProcessedAudioPayload payload = new ProcessedAudioPayload("Transcription test output");
    when(audioPreProcessor.process(any(byte[].class), eq("whisper"))).thenReturn(payload);

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertEquals("Transcription test output", result.get("analysis"));
  }

  @Test
  void execute_nullTranscriptInPayload_returnsEmptyString() throws Exception {
    Path audioFile = tempDir.resolve("test.wav");
    Files.write(audioFile, new byte[] {1, 2, 3});

    JobMessage message =
        new JobMessage(
            "job-1", "user-1", "audio", Map.of("filePath", audioFile.toAbsolutePath().toString()));
    Context context = Context.anonymous();

    ProcessedAudioPayload payload = new ProcessedAudioPayload(null);
    when(audioPreProcessor.process(any(byte[].class), any())).thenReturn(payload);

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertEquals("", result.get("analysis"));
  }
}
