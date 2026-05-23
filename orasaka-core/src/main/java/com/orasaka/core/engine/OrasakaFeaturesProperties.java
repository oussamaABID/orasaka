package com.orasaka.core.engine;

import java.util.Map;
import java.util.Objects;

record OrasakaFeaturesProperties(Map<String, FeatureConfig> features) {
  public OrasakaFeaturesProperties {
    features = (features == null) ? Map.of() : Map.copyOf(features);
  }

  public record FeatureConfig(
      boolean enabled,
      String label,
      String icon,
      String uriPath,
      String httpMethod,
      String payloadTemplate) {
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
