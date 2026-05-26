package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.application.pipeline.DynamicPipelineExecutor;
import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.ports.outbound.PipelineConfigProvider;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST controller for pipeline interceptor configuration management.
 *
 * <p>Exposes endpoints to list, reorder, toggle, and reset pipeline interceptor execution order.
 * After every mutation, evicts the pipeline chain cache to force immediate rebuild.
 */
@RestController
@RequestMapping("/api/v1/admin/pipeline/interceptors")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPipelineController {

  private static final Logger logger = LoggerFactory.getLogger(AdminPipelineController.class);

  private final PipelineConfigProvider configProvider;
  private final DynamicPipelineExecutor pipeline;

  public AdminPipelineController(
      PipelineConfigProvider configProvider, DynamicPipelineExecutor pipeline) {
    this.configProvider =
        Objects.requireNonNull(configProvider, "PipelineConfigProvider must not be null");
    this.pipeline = Objects.requireNonNull(pipeline, "DynamicPipelineExecutor must not be null");
  }

  /**
   * Retrieves all pipeline interceptor configurations ordered by execution order.
   *
   * @return Ordered list of interceptor configurations.
   */
  @GetMapping
  public ResponseEntity<List<InterceptorConfig>> getInterceptors() {
    return ResponseEntity.ok(pipeline.getCurrentConfig());
  }

  /**
   * Bulk-updates pipeline interceptor ordering and enabled state.
   *
   * @param configs The updated list of interceptor configurations.
   * @return The saved configurations.
   */
  @PutMapping
  public ResponseEntity<List<InterceptorConfig>> updateInterceptors(
      @RequestBody List<InterceptorConfig> configs) {
    logger.info("Admin pipeline update: saving {} interceptor configs.", configs.size());
    List<InterceptorConfig> saved = configProvider.saveAll(configs);
    pipeline.evictChainCache();
    return ResponseEntity.ok(saved);
  }

  /**
   * Resets all pipeline interceptor configurations to hardcoded defaults.
   *
   * @return The default configurations after reset.
   */
  @PostMapping("/reset")
  public ResponseEntity<List<InterceptorConfig>> resetToDefaults() {
    logger.info("Admin pipeline reset: restoring hardcoded defaults.");
    List<InterceptorConfig> defaults = pipeline.buildDefaultConfigs();
    configProvider.resetToDefaults(defaults);
    pipeline.evictChainCache();
    return ResponseEntity.ok(defaults);
  }
}
