package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.model.model.CatalogModelInfo;
import com.orasaka.core.domain.ports.inbound.CatalogModelService;
import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.gateway.infrastructure.config.ModelCatalogProperties;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Shared orchestration logic for REST media controllers (Image, Speech, etc.).
 *
 * <p>Centralizes model resolution, validation, directory preparation, job creation, and AMQP
 * publishing to eliminate cross-controller duplication.
 */
final class MediaJobOrchestrator {

  private static final Logger logger = LoggerFactory.getLogger(MediaJobOrchestrator.class);

  private MediaJobOrchestrator() {}

  /**
   * Resolves the active model name, falling back to the catalog default for the given category.
   *
   * @param requestModel explicit model from the request (may be null or blank).
   * @param category model category (e.g. "image", "speech").
   * @param fallbackModel hard-coded default if no catalog entry exists.
   * @param catalogModelService the catalog service for default model lookup.
   * @return the resolved model name.
   */
  static String resolveModel(
      String requestModel,
      String category,
      String fallbackModel,
      CatalogModelService catalogModelService) {
    if (requestModel != null && !requestModel.isBlank()) {
      return requestModel;
    }
    return catalogModelService
        .getDefaultModelByCategory(category)
        .map(CatalogModelInfo::modelName)
        .orElse(fallbackModel);
  }

  /**
   * Validates that the given model is listed in the model catalog for the given category.
   *
   * @throws InvalidRequestException if the model is not supported.
   */
  static void validateModel(
      String activeModel, String category, ModelCatalogProperties modelCatalogProperties) {
    boolean isSupported =
        modelCatalogProperties.getModels().getOrDefault(category, List.of()).stream()
            .anyMatch(m -> m.equalsIgnoreCase(activeModel));
    if (!isSupported) {
      throw new InvalidRequestException(
          "Model '"
              + activeModel
              + "' is not currently supported or enabled in the model catalog config registry.");
    }
  }

  /**
   * Creates input/output directories for a new job.
   *
   * @param uploadDir the base upload directory.
   * @param userId the user ID.
   * @param jobId the job ID.
   */
  static void prepareJobDirectories(String uploadDir, String userId, String jobId) {
    try {
      Path jobInputDir = Paths.get(uploadDir, userId, jobId, "input");
      Path jobOutputDir = Paths.get(uploadDir, userId, jobId, "output");
      Path jobTempDir = Paths.get(uploadDir, userId, jobId, "temp");
      Files.createDirectories(jobInputDir);
      Files.createDirectories(jobOutputDir);
      Files.createDirectories(jobTempDir);
    } catch (IOException e) {
      logger.error("Failed to initialize job directories", e);
    }
  }

  /**
   * Submits a media generation job: creates the job in the persistence layer and publishes the AMQP
   * message.
   *
   * @return the HTTP 202 response with jobId and status.
   */
  static ResponseEntity<Map<String, Object>> submitJob(
      String userId,
      String featureKey,
      String activeModel,
      Map<String, Object> payload,
      String uploadDir,
      JobService jobService,
      JobQueuePublisherService jobQueuePublisher) {
    String jobId = UUID.randomUUID().toString();
    prepareJobDirectories(uploadDir, userId, jobId);
    jobService.createJob(jobId, userId, featureKey, payload);
    JobMessage message = new JobMessage(jobId, userId, featureKey, activeModel, payload);
    jobQueuePublisher.publish(message);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(Map.of("jobId", jobId, "status", "PENDING"));
  }
}
