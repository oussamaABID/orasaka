package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.ports.inbound.CatalogModelService;
import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.infrastructure.config.ModelCatalogProperties;
import com.orasaka.gateway.infrastructure.support.PathResolver;
import com.orasaka.identity.domain.model.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for executing image generation tasks. */
@RestController
@RequestMapping("/api/v1")
public class ImageController {

  private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

  private final JobService jobService;
  private final JobQueuePublisherService jobQueuePublisher;
  private final ModelCatalogProperties modelCatalogProperties;
  private final CatalogModelService catalogModelService;
  private final String uploadDirProperty;

  /**
   * Constructs the image controller.
   *
   * @param jobService Job service from core.
   * @param jobQueuePublisher Job queue publisher.
   * @param modelCatalogProperties Model catalog properties for dynamic validation.
   * @param catalogModelService Dynamic model catalog service from core.
   * @param uploadDirProperty Upload directory path.
   */
  public ImageController(
      JobService jobService,
      JobQueuePublisherService jobQueuePublisher,
      ModelCatalogProperties modelCatalogProperties,
      CatalogModelService catalogModelService,
      @Value("${spring.servlet.multipart.location:var/orasaka-uploads}") String uploadDirProperty) {
    this.jobService = Objects.requireNonNull(jobService, "JobService must not be null");
    this.jobQueuePublisher =
        Objects.requireNonNull(jobQueuePublisher, "JobQueuePublisherService must not be null");
    this.modelCatalogProperties =
        Objects.requireNonNull(modelCatalogProperties, "ModelCatalogProperties must not be null");
    this.catalogModelService =
        Objects.requireNonNull(catalogModelService, "CatalogModelService must not be null");
    this.uploadDirProperty = PathResolver.resolveToString(uploadDirProperty);
  }

  /** Request DTO for Image generation parameters. */
  public static record ImageGenerationRequest(String prompt, String model) {
    public ImageGenerationRequest {
      Objects.requireNonNull(prompt, "Prompt parameter is required");
    }
  }

  /**
   * REST endpoint to submit an image generation task.
   *
   * @param request The image request parameters.
   * @param user The authenticated user principal.
   * @return Safe HTTP response containing the Job ID.
   */
  @PostMapping("/ai/image")
  public ResponseEntity<Map<String, Object>> generateImage(
      @RequestBody ImageGenerationRequest request, @AuthenticationPrincipal User user) {
    logger.debug("Received image generation request for prompt: {}", request.prompt());
    String userId = user.id().toString();

    String activeModel =
        MediaJobOrchestrator.resolveModel(
            request.model(), "image", "stable-diffusion-xl", catalogModelService);
    MediaJobOrchestrator.validateModel(activeModel, "image", modelCatalogProperties);

    Map<String, Object> payload = new HashMap<>();
    payload.put("prompt", request.prompt());
    payload.put("model", activeModel);

    return MediaJobOrchestrator.submitJob(
        userId,
        "orasaka.core.media.image",
        activeModel,
        payload,
        uploadDirProperty,
        jobService,
        jobQueuePublisher);
  }
}
