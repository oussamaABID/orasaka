package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.application.processing.ProcessedVideoPayload;
import com.orasaka.core.application.processing.VideoPreProcessor;
import com.orasaka.core.domain.model.Context;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoAnalysisStrategyTest {

  @TempDir Path tempDir;

  @Mock private VideoPreProcessor videoPreProcessor;

  private VideoAnalysisStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new VideoAnalysisStrategy(videoPreProcessor);
  }

  @Test
  void supports_withVideoAnalysisKey_returnsTrue() {
    assertTrue(strategy.supports("orasaka.core.media.video.analysis"));
    assertFalse(strategy.supports("orasaka.core.media.video.generation"));
    assertFalse(strategy.supports("orasaka.core.chat.text"));
    assertFalse(strategy.supports(null));
  }

  @Test
  void execute_missingFilePath_throwsException() {
    JobMessage message =
        new JobMessage("job-1", "user-1", "orasaka.core.media.video.analysis", Map.of());
    Context context = Context.anonymous();

    assertThrows(JobExecutionException.class, () -> strategy.execute(message, context));
  }

  @Test
  void execute_fileNotFound_throwsException() {
    JobMessage message =
        new JobMessage(
            "job-1",
            "user-1",
            "orasaka.core.media.video.analysis",
            Map.of("filePath", "missing.mp4"));
    Context context = Context.anonymous();

    assertThrows(JobExecutionException.class, () -> strategy.execute(message, context));
  }

  @Test
  void execute_successfulAnalysis_returnsTranscriptAndKeyframeCount() throws Exception {
    Path videoFile = tempDir.resolve("test.mp4");
    Files.write(videoFile, new byte[] {1, 2, 3});

    JobMessage message =
        new JobMessage(
            "job-1",
            "user-1",
            "orasaka.core.media.video.analysis",
            Map.of("filePath", videoFile.toAbsolutePath().toString(), "model", "gemini-video"));
    Context context = Context.anonymous();

    ProcessedVideoPayload payload =
        new ProcessedVideoPayload(
            "Video transcription test output", List.of(new byte[] {1}, new byte[] {2}));
    when(videoPreProcessor.process(any(byte[].class), eq("gemini-video"))).thenReturn(payload);

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertEquals("Video transcription test output", result.get("transcript"));
    assertEquals(2, result.get("keyframeCount"));
  }
}
