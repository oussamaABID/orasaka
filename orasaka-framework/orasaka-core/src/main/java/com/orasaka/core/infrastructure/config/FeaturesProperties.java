package com.orasaka.core.infrastructure.config;

import com.orasaka.core.application.engine.GraphEngine;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration properties defining the available AI features (capabilities) for the Operation
 * Graph.
 *
 * <p>Maps to the {@code orasaka.features} prefix in {@code application.yml}. Each key in the {@code
 * features} map represents a unique capability ID (e.g., {@code "orasaka.core.chat.text"}), and
 * each value is a {@link FeatureConfig} describing its display metadata and execution details.
 *
 * <p>Disabled features ({@code enabled: false}) are rendered as {@code INVISIBLE} nodes in the
 * Operation Graph, completely hidden from the CLI and UI.
 *
 * @param features Immutable map of capability ID → {@link FeatureConfig}.
 * @see GraphEngine
 */
public record FeaturesProperties(Map<String, FeatureConfig> features) {
  /** Compact constructor — defensively copies the map to enforce immutability. */
  public FeaturesProperties {
    features = (features == null) ? Map.of() : Map.copyOf(features);
  }

  /**
   * Configuration for a single AI feature/capability in the Operation Graph.
   *
   * @param enabled Whether this feature is active and visible.
   * @param label Human-readable display label (required, non-blank).
   * @param icon Icon keyword for UI rendering (required, non-blank).
   * @param uriPath The REST endpoint path for execution (required, non-blank).
   * @param httpMethod The HTTP method for execution (required, non-blank).
   * @param payloadTemplate Optional JSON payload template with {@code ${param}} placeholders.
   */
  public record FeatureConfig(
      boolean enabled,
      String label,
      String icon,
      String uriPath,
      String httpMethod,
      String payloadTemplate) {
    /** Compact constructor — validates all required fields are non-null and non-blank. */
    public FeatureConfig {
      Objects.requireNonNull(label, "Feature label cannot be null");
      if (label.isBlank()) {
        throw new IllegalArgumentException("Feature label cannot be blank");
      }
      Objects.requireNonNull(icon, "Feature icon cannot be null");
      if (icon.isBlank()) {
        throw new IllegalArgumentException("Feature icon cannot be blank");
      }
      Objects.requireNonNull(uriPath, "Feature URI path cannot be null");
      if (uriPath.isBlank()) {
        throw new IllegalArgumentException("Feature URI path cannot be blank");
      }
      Objects.requireNonNull(httpMethod, "Feature HTTP method cannot be null");
      if (httpMethod.isBlank()) {
        throw new IllegalArgumentException("Feature HTTP method cannot be blank");
      }
    }
  }
}
