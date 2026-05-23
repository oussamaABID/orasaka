package com.orasaka.core.support;

/**
 * Immutable Spring Application Event emitted when an Orasaka chat request completes successfully.
 *
 * @param request The original request.
 * @param response The generated response.
 */
public record OrasakaChatCompletedEvent(OrasakaChatRequest request, OrasakaChatResponse response) {
  public OrasakaChatCompletedEvent {
    java.util.Objects.requireNonNull(request, "Request cannot be null");
    java.util.Objects.requireNonNull(response, "Response cannot be null");
  }
}
