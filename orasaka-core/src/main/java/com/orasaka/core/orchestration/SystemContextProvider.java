package com.orasaka.core.orchestration;

import java.util.Map;

/**
 * Interface to provide external system context signals to the prompt matrix pipeline.
 *
 * <p>Implemented by downstream modules (e.g. {@code orasaka-tools}) to register ambient details
 * without creating tight circular dependencies on {@code orasaka-core}.
 */
public interface SystemContextProvider {

  /**
   * Resolves and returns system context metrics or variables.
   *
   * @return A map of system key-value metrics.
   */
  Map<String, Object> getSystemContext();
}
