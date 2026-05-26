package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.model.CatalogModelInfo;
import com.orasaka.core.domain.ports.inbound.CatalogModelService;
import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.gateway.infrastructure.config.ModelCatalogProperties;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class MediaJobOrchestratorTest {

  @Test
  void testResolveModel() {
    CatalogModelService catalogModelService = mock(CatalogModelService.class);

    // Case 1: requestModel is explicitly provided
    String resolved =
        MediaJobOrchestrator.resolveModel("custom-model", "image", "fallback", catalogModelService);
    assertEquals("custom-model", resolved);
    verifyNoInteractions(catalogModelService);

    // Case 2: requestModel is null, catalog default exists
    CatalogModelInfo defaultModel =
        new CatalogModelInfo(1, "catalog-default", "Default Model", "image", "{}", true);
    when(catalogModelService.getDefaultModelByCategory("image"))
        .thenReturn(Optional.of(defaultModel));
    resolved = MediaJobOrchestrator.resolveModel(null, "image", "fallback", catalogModelService);
    assertEquals("catalog-default", resolved);

    // Case 3: requestModel is blank, catalog default does not exist
    when(catalogModelService.getDefaultModelByCategory("image")).thenReturn(Optional.empty());
    resolved = MediaJobOrchestrator.resolveModel("  ", "image", "fallback", catalogModelService);
    assertEquals("fallback", resolved);
  }

  @Test
  void testValidateModel() {
    ModelCatalogProperties properties = mock(ModelCatalogProperties.class);
    Map<String, List<String>> models = Map.of("image", List.of("model-A", "model-B"));
    when(properties.getModels()).thenReturn(models);

    // Case 1: Supported model (case-insensitive)
    assertDoesNotThrow(() -> MediaJobOrchestrator.validateModel("model-a", "image", properties));

    // Case 2: Unsupported model
    assertThrows(
        InvalidRequestException.class,
        () -> MediaJobOrchestrator.validateModel("unsupported", "image", properties));
  }

  @Test
  void testPrepareJobDirectoriesSuccess(@TempDir Path tempDir) {
    String uploadDir = tempDir.resolve("uploads").toString();
    String userId = "user-123";
    String jobId = "job-456";

    MediaJobOrchestrator.prepareJobDirectories(uploadDir, userId, jobId);

    Path jobBase = tempDir.resolve("uploads").resolve(userId).resolve(jobId);
    assertTrue(Files.exists(jobBase.resolve("input")));
    assertTrue(Files.exists(jobBase.resolve("output")));
    assertTrue(Files.exists(jobBase.resolve("temp")));
  }

  @Test
  void testSubmitJobSuccess(@TempDir Path tempDir) {
    String uploadDir = tempDir.resolve("uploads").toString();
    JobService jobService = mock(JobService.class);
    JobQueuePublisherService jobQueuePublisher = mock(JobQueuePublisherService.class);
    Map<String, Object> payload = new HashMap<>();

    ResponseEntity<Map<String, Object>> response =
        MediaJobOrchestrator.submitJob(
            "user-123", "image", "model-A", payload, uploadDir, jobService, jobQueuePublisher);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().containsKey("jobId"));
    assertEquals("PENDING", response.getBody().get("status"));

    verify(jobService).createJob(anyString(), eq("user-123"), eq("image"), eq(payload));
    verify(jobQueuePublisher).publish(any(JobMessage.class));
  }
}
