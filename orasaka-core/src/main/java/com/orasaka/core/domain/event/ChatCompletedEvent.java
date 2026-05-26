package com.orasaka.core.domain.event;

import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.domain.model.chat.InternalChatResponse;
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
