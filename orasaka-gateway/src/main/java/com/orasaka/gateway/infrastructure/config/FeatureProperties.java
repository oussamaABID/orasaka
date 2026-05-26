package com.orasaka.gateway.infrastructure.config;

import java.util.Objects;

/** Record representing properties of an individual feature capability. */
public record FeatureProperties(
    boolean enabled,
    String label,
    String icon,
    String uriPath,
    String httpMethod,
    String payloadTemplate) {
  public FeatureProperties {
    Objects.requireNonNull(label, "label must not be null");
    Objects.requireNonNull(icon, "icon must not be null");
    Objects.requireNonNull(uriPath, "uriPath must not be null");
    Objects.requireNonNull(httpMethod, "httpMethod must not be null");
    Objects.requireNonNull(payloadTemplate, "payloadTemplate must not be null");
  }
}
