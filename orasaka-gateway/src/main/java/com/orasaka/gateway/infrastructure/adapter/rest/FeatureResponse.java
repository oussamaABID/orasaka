package com.orasaka.gateway.infrastructure.adapter.rest;

import java.util.Objects;

/** Record representing feature details exposed to the frontend clients during bootstrap. */
public record FeatureResponse(
    String id,
    String label,
    String icon,
    String uriPath,
    String httpMethod,
    String payloadTemplate) {
  public FeatureResponse {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(label, "label must not be null");
    Objects.requireNonNull(icon, "icon must not be null");
    Objects.requireNonNull(uriPath, "uriPath must not be null");
    Objects.requireNonNull(httpMethod, "httpMethod must not be null");
    Objects.requireNonNull(payloadTemplate, "payloadTemplate must not be null");
  }
}
