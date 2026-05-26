package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.video.VideoRequest;
import com.orasaka.core.domain.model.video.VideoResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoGenerationStrategyTest {

  @TempDir Path tempDir;

  @Mock private AiClient aiClient;
  @Mock private CatalogModelManager catalogModelManager;

  private VideoGenerationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new VideoGenerationStrategy(aiClient, catalogModelManager, tempDir.toString());
  }

  @Test
  void supports_withVideoKey_returnsTrue() {
    assertTrue(strategy.supports("orasaka.core.media.video.generation"));
    assertFalse(strategy.supports("orasaka.core.media.video.analysis"));
    assertFalse(strategy.supports("orasaka.core.chat.text"));
    assertFalse(strategy.supports(null));
  }

  @Test
  void execute_successfulExecution_generatesVideoFile() throws Exception {
    String jobId = "job-123";
    String userId = "user-456";

    Path imageFile = tempDir.resolve("input.png");
    Files.write(imageFile, new byte[] {1, 2, 3});

    Map<String, Object> payload = new HashMap<>();
    payload.put("prompt", "A flying car");
    payload.put("imagePath", imageFile.toAbsolutePath().toString());
    payload.put("durationSeconds", 8);
    JobMessage message = new JobMessage(jobId, userId, "video.generation", "", payload);
    Context context = Context.anonymous();

    byte[] videoBytes = new byte[] {10, 20, 30};
    VideoResponse videoResponse = new VideoResponse(videoBytes, "mp4", Map.of("latency", 1500L));
    when(aiClient.video(any(VideoRequest.class))).thenReturn(videoResponse);
    when(catalogModelManager.getDefaultModelByCategory("video")).thenReturn(Optional.empty());

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertTrue(result.get("url").toString().contains("job-123/output/video.mp4"));
    assertEquals("mp4", result.get("format"));
    assertEquals(Map.of("latency", 1500L), result.get("metrics"));

    ArgumentCaptor<VideoRequest> captor = ArgumentCaptor.forClass(VideoRequest.class);
    verify(aiClient).video(captor.capture());
    VideoRequest capturedRequest = captor.getValue();
    assertEquals("A flying car", capturedRequest.prompt());
    assertEquals(8, capturedRequest.durationSeconds());
    assertEquals("stable-video-diffusion-img2vid-xt", capturedRequest.model());
    assertEquals(imageFile.toAbsolutePath().toString(), capturedRequest.inputPath());
  }

  @Test
  void execute_withModelFromCatalog_usesCatalogModel() throws Exception {
    String jobId = "job-123";
    String userId = "user-456";

    Map<String, Object> payload = new HashMap<>();
    payload.put("text", "A futuristic city");
    JobMessage message = new JobMessage(jobId, userId, "video.generation", payload);
    message = spy(message);
    when(message.model()).thenReturn(null); // default lookup

    Context context = Context.anonymous();

    byte[] videoBytes = new byte[] {10, 20, 30};
    VideoResponse videoResponse = new VideoResponse(videoBytes, "mp4", Map.of());
    when(aiClient.video(any(VideoRequest.class))).thenReturn(videoResponse);

    CatalogModelDto catalogModel =
        new CatalogModelDto(1, "catalog-video-model", "catalog-video-model", "video", null, true);
    when(catalogModelManager.getDefaultModelByCategory("video"))
        .thenReturn(Optional.of(catalogModel));

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    ArgumentCaptor<VideoRequest> captor = ArgumentCaptor.forClass(VideoRequest.class);
    verify(aiClient).video(captor.capture());
    assertEquals("catalog-video-model", captor.getValue().model());
  }

  @Test
  void execute_missingPrompt_throwsException() {
    JobMessage message = new JobMessage("job-123", "user-456", "video.generation", Map.of());
    Context context = Context.anonymous();

    assertThrows(IllegalArgumentException.class, () -> strategy.execute(message, context));
  }
}
