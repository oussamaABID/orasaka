package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.image.ImageRequest;
import com.orasaka.core.domain.model.image.ImageResponse;
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
class ImageGenerationStrategyTest {

  @TempDir Path tempDir;

  @Mock private AiClient aiClient;
  @Mock private CatalogModelManager catalogModelManager;

  private ImageGenerationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new ImageGenerationStrategy(aiClient, catalogModelManager, tempDir.toString());
  }

  @Test
  void supports_withImageKey_returnsTrue() {
    assertTrue(strategy.supports("orasaka.core.media.image.generation"));
    assertFalse(strategy.supports("orasaka.core.chat.text"));
    assertFalse(strategy.supports(null));
  }

  @Test
  void execute_withImageData_savesToFile() throws Exception {
    String jobId = "job-1";
    String userId = "user-1";

    Map<String, Object> payload = Map.of("prompt", "A cute kitten");
    JobMessage message = new JobMessage(jobId, userId, "image.generation", "", payload);
    Context context = Context.anonymous();

    byte[] imgData = new byte[] {1, 2, 3, 4};
    ImageResponse response = new ImageResponse(imgData, null, "png");
    when(aiClient.image(any(ImageRequest.class))).thenReturn(response);
    when(catalogModelManager.getDefaultModelByCategory("image")).thenReturn(Optional.empty());

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertTrue(result.get("url").toString().contains("job-1/output/image.png"));
    assertEquals("png", result.get("format"));
  }

  @Test
  void execute_withDataUrl_decodesAndSavesToFile() throws Exception {
    String jobId = "job-1";
    String userId = "user-1";

    Map<String, Object> payload = Map.of("prompt", "A cute kitten");
    JobMessage message = new JobMessage(jobId, userId, "image.generation", "", payload);
    Context context = Context.anonymous();

    // "data:image/png;base64,AQID" decodes to [1, 2, 3]
    ImageResponse response = new ImageResponse(null, "data:image/png;base64,AQID", "png");
    when(aiClient.image(any(ImageRequest.class))).thenReturn(response);
    when(catalogModelManager.getDefaultModelByCategory("image")).thenReturn(Optional.empty());

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertTrue(result.get("url").toString().contains("job-1/output/image.png"));
    assertEquals("png", result.get("format"));
  }

  @Test
  void execute_withHttpUrl_returnsUrlDirectly() throws Exception {
    String jobId = "job-1";
    String userId = "user-1";

    Map<String, Object> payload = Map.of("prompt", "A cute kitten");
    JobMessage message = new JobMessage(jobId, userId, "image.generation", "", payload);
    Context context = Context.anonymous();

    ImageResponse response = new ImageResponse(null, "https://example.com/generated.png", "png");
    when(aiClient.image(any(ImageRequest.class))).thenReturn(response);
    when(catalogModelManager.getDefaultModelByCategory("image")).thenReturn(Optional.empty());

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertEquals("https://example.com/generated.png", result.get("url"));
    assertEquals("png", result.get("format"));
  }

  @Test
  void execute_withInvalidDataUrl_doesNotCrash() throws Exception {
    String jobId = "job-1";
    String userId = "user-1";

    Map<String, Object> payload = Map.of("prompt", "A cute kitten");
    JobMessage message = new JobMessage(jobId, userId, "image.generation", "", payload);
    Context context = Context.anonymous();

    ImageResponse response = new ImageResponse(null, "data:image/png;base64,!!!invalid!!!", "png");
    when(aiClient.image(any(ImageRequest.class))).thenReturn(response);
    when(catalogModelManager.getDefaultModelByCategory("image")).thenReturn(Optional.empty());

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertEquals("data:image/png;base64,!!!invalid!!!", result.get("url"));
  }
}
