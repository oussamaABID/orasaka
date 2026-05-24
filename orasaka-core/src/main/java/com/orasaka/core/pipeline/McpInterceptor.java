package com.orasaka.core.pipeline;

import com.orasaka.core.support.InternalChatRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

/**
 * MCP (Model Context Protocol) interceptor — enriches the prompt with external context resolved via
 * the {@link McpOrchestrator}.
 *
 * @see McpOrchestrator#resolveExternalContext()
 * @since 1.0.0
 */
@Component
class McpInterceptor implements ContextInterceptor {
  private final McpOrchestrator mcpOrchestrator;

  public McpInterceptor(McpOrchestrator mcpOrchestrator) {
    this.mcpOrchestrator =
        Objects.requireNonNull(mcpOrchestrator, "McpOrchestrator must not be null");
  }

  @Override
  public ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    Optional.ofNullable(mcpOrchestrator.resolveExternalContext())
        .filter(ctx -> !ctx.isBlank())
        .ifPresent(ctx -> messages.add(new SystemMessage("MCP Context: " + ctx)));
    return options;
  }
}
