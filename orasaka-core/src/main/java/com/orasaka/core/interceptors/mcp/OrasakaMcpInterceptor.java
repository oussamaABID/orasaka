package com.orasaka.core.interceptors.mcp;

import com.orasaka.core.interceptors.OrasakaContextInterceptor;
import com.orasaka.core.model.OrasakaChatRequest;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

@Component
public class OrasakaMcpInterceptor implements OrasakaContextInterceptor {

  private final McpOrchestrator mcpOrchestrator;

  public OrasakaMcpInterceptor(McpOrchestrator mcpOrchestrator) {
    this.mcpOrchestrator = mcpOrchestrator;
  }

  @Override
  public ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    if (mcpOrchestrator != null) {
      String mcpContext = mcpOrchestrator.resolveExternalContext();
      if (mcpContext != null && !mcpContext.isBlank()) {
        messages.add(new SystemMessage("MCP Context: " + mcpContext));
      }
    }
    return options;
  }
}
