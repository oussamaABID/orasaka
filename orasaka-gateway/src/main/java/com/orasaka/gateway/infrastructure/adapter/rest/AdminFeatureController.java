package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.persistence.infrastructure.adapter.persistence.entity.FeatureFlagEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.FeatureFlagRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only REST controller exposing capabilities registry feature flags. */
@RestController
@RequestMapping("/api/v1/admin/features")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminFeatureController {

  private final FeatureFlagRepository repository;

  public AdminFeatureController(FeatureFlagRepository repository) {
    this.repository = Objects.requireNonNull(repository, "FeatureFlagRepository must not be null");
  }

  /** DTO payload for feature flag updates — prevents mass assignment of JPA entity fields. */
  record FeatureFlagPayload(Boolean isEnabled) {}

  /**
   * Retrieves all feature flags from the database.
   *
   * @return List of feature flags.
   */
  @GetMapping
  public ResponseEntity<List<FeatureFlagEntity>> getFeatures() {
    return ResponseEntity.ok(repository.findAll());
  }

  /**
   * Updates or inserts a feature flag state.
   *
   * @param featureKey The feature key identifier.
   * @param payload The updated feature flag DTO payload.
   * @return The saved feature flag entity.
   */
  @PutMapping("/{featureKey}")
  public ResponseEntity<FeatureFlagEntity> updateFeature(
      @PathVariable String featureKey, @RequestBody FeatureFlagPayload payload) {
    FeatureFlagEntity entity = repository.findById(featureKey).orElse(new FeatureFlagEntity());
    entity.setFeatureKey(featureKey);
    entity.setIsEnabled(payload.isEnabled());
    return ResponseEntity.ok(repository.save(entity));
  }
}
