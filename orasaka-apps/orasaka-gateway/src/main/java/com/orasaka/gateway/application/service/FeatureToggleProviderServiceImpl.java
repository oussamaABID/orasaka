package com.orasaka.gateway.application.service;

import com.orasaka.core.domain.ports.outbound.FeatureToggleProvider;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.FeatureFlagEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.FeatureFlagRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of the FeatureToggleProvider bridging core and database feature flags. */
@Service
@Transactional(readOnly = true)
class FeatureToggleProviderServiceImpl implements FeatureToggleProvider {

  private final FeatureFlagRepository repository;

  FeatureToggleProviderServiceImpl(FeatureFlagRepository repository) {
    this.repository = Objects.requireNonNull(repository, "FeatureFlagRepository cannot be null");
  }

  @Override
  public Optional<Boolean> isEnabled(String featureKey) {
    Objects.requireNonNull(featureKey, "Feature key cannot be null");
    return repository.findById(featureKey).map(FeatureFlagEntity::getIsEnabled);
  }
}
