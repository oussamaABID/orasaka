package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.core.infrastructure.config.CoreProperties;
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
class VisionAnalysisStrategyTest {

  @TempDir Path tempDir;

  @Mock private AiClient aiClient;
  @Mock private CatalogModelManager catalogModelManager;

  private CoreProperties coreProperties;
  private VisionAnalysisStrategy strategy;

  @BeforeEach
  void setUp() {
    coreProperties =
        new CoreProperties(
            "ollama",
            null,
            null,
            null,
            null,
            null,
            new CoreProperties.VisionConfig("openai", "gpt-4o-vision"),
            null);
    strategy = new VisionAnalysisStrategy(aiClient, coreProperties, catalogModelManager);
  }

  @Test
  void supports_withVisionKey_returnsTrue() {
    assertTrue(strategy.supports("orasaka.core.media.vision.analysis"));
    assertFalse(strategy.supports("orasaka.core.chat.text"));
    assertFalse(strategy.supports(null));
  }

  @Test
  void execute_missingFilePath_throwsException() {
    JobMessage message = new JobMessage("job-1", "user-1", "vision", Map.of());
    Context context = Context.anonymous();

    JobExecutionException exception =
        assertThrows(JobExecutionException.class, () -> strategy.execute(message, context));
    assertEquals("Payload does not contain filePath field", exception.getMessage());
  }

  @Test
  void execute_fileNotFound_throwsException() {
    JobMessage message =
        new JobMessage("job-1", "user-1", "vision", Map.of("filePath", "non-existent-file.png"));
    Context context = Context.anonymous();

    JobExecutionException exception =
        assertThrows(JobExecutionException.class, () -> strategy.execute(message, context));
    assertTrue(exception.getMessage().contains("Image file not found"));
  }

  @Test
  void execute_successfulExecution_returnsAnalysisResult() throws Exception {
    Path imageFile = tempDir.resolve("test-image.png");
    Files.write(imageFile, new byte[] {1, 2, 3});

    Map<String, Object> payload = new HashMap<>();
    payload.put("filePath", imageFile.toAbsolutePath().toString());
    payload.put("prompt", "Describe this image");
    JobMessage message = new JobMessage("job-1", "user-1", "vision", payload);
    Context context = Context.anonymous();

    ChatResponse chatResponse = new ChatResponse("A beautiful poster", "conv-1", Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(chatResponse);
    when(catalogModelManager.getDefaultModelByCategory("vision")).thenReturn(Optional.empty());

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertEquals("A beautiful poster", result.get("analysis"));

    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(captor.capture());
    ChatRequest capturedRequest = captor.getValue();
    assertTrue(capturedRequest.prompt().startsWith("Describe this image [posterBase64: "));
    assertEquals("openai", capturedRequest.settings().get("provider"));
    assertEquals("gpt-4o-vision", capturedRequest.settings().get("model"));
  }

  @Test
  void execute_invalidVisionResponse_usesFallback() throws Exception {
    Path imageFile = tempDir.resolve("test-image.png");
    Files.write(imageFile, new byte[] {1, 2, 3});

    Map<String, Object> payload = new HashMap<>();
    payload.put("filePath", imageFile.toAbsolutePath().toString());
    JobMessage message = new JobMessage("job-1", "user-1", "vision", payload);
    Context context = Context.anonymous();

    ChatResponse chatResponse = new ChatResponse("I don't see any image here", "conv-1", Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(chatResponse);
    when(catalogModelManager.getDefaultModelByCategory("vision")).thenReturn(Optional.empty());

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    assertTrue(
        result.get("analysis").toString().contains("Visual analysis of the uploaded poster shows"));
  }

  @Test
  void execute_withModelOverride_usesOverride() throws Exception {
    Path imageFile = tempDir.resolve("test-image.png");
    Files.write(imageFile, new byte[] {1, 2, 3});

    Map<String, Object> payload = new HashMap<>();
    payload.put("filePath", imageFile.toAbsolutePath().toString());
    payload.put("model", "custom-vision-model");
    JobMessage message = new JobMessage("job-1", "user-1", "vision", payload);
    Context context = Context.anonymous();

    ChatResponse chatResponse = new ChatResponse("A beautiful poster", "conv-1", Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(chatResponse);

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(captor.capture());
    assertEquals("custom-vision-model", captor.getValue().settings().get("model"));
  }

  @Test
  void execute_withCatalogDefaultModel_usesCatalogModel() throws Exception {
    Path imageFile = tempDir.resolve("test-image.png");
    Files.write(imageFile, new byte[] {1, 2, 3});

    Map<String, Object> payload = new HashMap<>();
    payload.put("filePath", imageFile.toAbsolutePath().toString());
    JobMessage message = new JobMessage("job-1", "user-1", "vision", payload);
    Context context = Context.anonymous();

    ChatResponse chatResponse = new ChatResponse("A beautiful poster", "conv-1", Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(chatResponse);
    CatalogModelDto catalogModel =
        new CatalogModelDto(
            1, "catalog-vision-model", "catalog-vision-model", "vision", null, true);
    when(catalogModelManager.getDefaultModelByCategory("vision"))
        .thenReturn(Optional.of(catalogModel));

    Map<String, Object> result = strategy.execute(message, context);

    assertNotNull(result);
    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(captor.capture());
    assertEquals("catalog-vision-model", captor.getValue().settings().get("model"));
  }

  @Test
  void execute_withNullProperties_usesFallbackDefaults() throws Exception {
    Path imageFile = tempDir.resolve("test-image.png");
    Files.write(imageFile, new byte[] {1, 2, 3});

    Map<String, Object> payload = new HashMap<>();
    payload.put("filePath", imageFile.toAbsolutePath().toString());
    JobMessage message = new JobMessage("job-1", "user-1", "vision", payload);
    Context context = Context.anonymous();

    ChatResponse chatResponse = new ChatResponse("A beautiful poster", "conv-1", Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(chatResponse);
    when(catalogModelManager.getDefaultModelByCategory("vision")).thenReturn(Optional.empty());

    VisionAnalysisStrategy fallbackStrategy =
        new VisionAnalysisStrategy(aiClient, null, catalogModelManager);
    Map<String, Object> result = fallbackStrategy.execute(message, context);

    assertNotNull(result);
    ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(captor.capture());
    assertEquals("ollama", captor.getValue().settings().get("provider"));
    assertEquals("llama3.2-vision:latest", captor.getValue().settings().get("model"));
  }
}
