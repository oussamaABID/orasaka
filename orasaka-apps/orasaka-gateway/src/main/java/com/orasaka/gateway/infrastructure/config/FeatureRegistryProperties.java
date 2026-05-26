package com.orasaka.gateway.infrastructure.config;

import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties record for Orasaka metadata-driven feature registry settings. */
@ConfigurationProperties(prefix = "orasaka")
public record FeatureRegistryProperties(Map<String, FeatureProperties> features) {
  public FeatureRegistryProperties {
    Objects.requireNonNull(features, "features must not be null");
  }
}
