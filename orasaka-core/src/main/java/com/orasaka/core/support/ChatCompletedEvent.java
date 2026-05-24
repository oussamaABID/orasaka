package com.orasaka.core.support;

import java.util.Objects;

/**
 * Immutable Spring Application Event emitted when an Orasaka chat request completes successfully.
 *
 * @param request The original request.
 * @param response The generated response.
 */
public record ChatCompletedEvent(InternalChatRequest request, InternalChatResponse response) {
  public ChatCompletedEvent {
    Objects.requireNonNull(request, "Request cannot be null");
    Objects.requireNonNull(response, "Response cannot be null");
  }
}
