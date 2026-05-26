package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.model.ValidationPipelineConfiguration;
import com.orasaka.core.domain.ports.outbound.ValidationPipelineRepository;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST controller for validation pipeline tier configuration management.
 *
 * <p>Exposes endpoints to list and update the 4-tier validation matrix configuration. Follows the
 * established {@link AdminPipelineController} pattern with RBAC enforcement.
 *
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/v1/admin/validation-pipeline")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ValidationAdminController {

  private static final Logger logger = LoggerFactory.getLogger(ValidationAdminController.class);

  private final ValidationPipelineRepository validationPipelineRepository;

  public ValidationAdminController(ValidationPipelineRepository validationPipelineRepository) {
    this.validationPipelineRepository =
        Objects.requireNonNull(
            validationPipelineRepository, "ValidationPipelineRepository must not be null");
  }

  /**
   * Retrieves all validation pipeline tier configurations ordered by execution order.
   *
   * @return Ordered list of validation tier configurations.
   */
  @GetMapping
  public ResponseEntity<List<ValidationPipelineConfiguration>> getValidationPipeline() {
    return ResponseEntity.ok(validationPipelineRepository.findAllOrderedByExecution());
  }

  /**
   * Bulk-updates validation pipeline tier ordering and enabled state.
   *
   * @param configs The updated list of tier configurations.
   * @return The saved configurations.
   */
  @PutMapping
  public ResponseEntity<List<ValidationPipelineConfiguration>> updateValidationPipeline(
      @RequestBody List<ValidationPipelineConfiguration> configs) {
    logger.info("Admin validation pipeline update: saving {} tier configs.", configs.size());
    List<ValidationPipelineConfiguration> saved = validationPipelineRepository.saveAll(configs);
    return ResponseEntity.ok(saved);
  }
}
