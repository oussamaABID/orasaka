package com.orasaka.core.interceptors.tool;

import com.orasaka.core.interceptors.OrasakaContextInterceptor;
import com.orasaka.core.model.OrasakaChatRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

@Component // Note: Since we don't have org.springframework.stereotype.Component imported, let's add
// the import.
public class OrasakaToolInterceptor implements OrasakaContextInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaToolInterceptor.class);

  private final OrasakaToolRegistry toolRegistry;

  public OrasakaToolInterceptor(OrasakaToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  @Override
  public ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    int toolCount = 0;
    if (toolRegistry != null && !toolRegistry.getRegisteredTools().isEmpty()) {
      options = attachTools(options);
      toolCount = toolRegistry.getRegisteredTools().size();
    }
    logger.debug("Attached {} tools to ChatOptions", toolCount);
    return options;
  }

  private ChatOptions attachTools(ChatOptions options) {
    Set<String> toolNames =
        toolRegistry.getRegisteredTools().stream()
            .map(t -> t.getToolDefinition().name())
            .collect(Collectors.toSet());

    return switch (options) {
      case OllamaChatOptions ollama ->
          OllamaChatOptions.builder()
              .model(ollama.getModel())
              .temperature(ollama.getTemperature())
              .numPredict(ollama.getNumPredict())
              .toolNames(toolNames)
              .build();
      case OpenAiChatOptions openai ->
          OpenAiChatOptions.builder()
              .model(openai.getModel())
              .temperature(openai.getTemperature())
              .maxTokens(openai.getMaxTokens())
              .toolNames(toolNames)
              .build();
      default -> options;
    };
  }
}
