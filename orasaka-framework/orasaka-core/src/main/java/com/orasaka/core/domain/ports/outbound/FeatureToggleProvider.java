package com.orasaka.core.domain.ports.outbound;

import java.util.Optional;

/** Public contract for resolving database feature toggles. */
public interface FeatureToggleProvider {

  /**
   * Resolves the database override state for a feature.
   *
   * @param featureKey The capability ID key.
   * @return Optional containing the enabled flag if configured.
   */
  Optional<Boolean> isEnabled(String featureKey);
}
