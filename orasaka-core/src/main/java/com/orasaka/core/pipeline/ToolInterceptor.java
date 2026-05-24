package com.orasaka.core.pipeline;

import com.orasaka.core.support.InternalChatRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Tool execution interceptor — analyzes prompt content to selectively attach relevant {@link
 * ToolCallback} definitions to the chat options.
 *
 * <p>Uses keyword-based demand detection to determine which tools are relevant for the current
 * prompt (e.g., image analysis for "poster", audio analysis for "audio").
 *
 * @see ToolRegistry
 * @see ContextInterceptor
 * @since 1.0.0
 */
@Component
class ToolInterceptor implements ContextInterceptor {
  private final ToolRegistry toolRegistry;

  public ToolInterceptor(ToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  @Override
  public ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    if (toolRegistry == null || toolRegistry.getRegisteredTools().isEmpty()) return options;

    List<ToolCallback> demandedTools = collectDemandedTools(request, promptText);
    if (demandedTools.isEmpty()) return options;

    return attachTools(options, demandedTools);
  }

  private List<ToolCallback> collectDemandedTools(InternalChatRequest request, String promptText) {
    String lowerContext = extractFullText(request, promptText).toLowerCase();
    List<ToolCallback> demanded = new ArrayList<>();

    for (ToolCallback tool : toolRegistry.getRegisteredTools()) {
      String name = tool.getToolDefinition().name();
      if ("analyzePoster".equals(name) && demandsPosterTool(lowerContext)) demanded.add(tool);
      else if ("analyzeAudioExtract".equals(name) && demandsAudioTool(lowerContext))
        demanded.add(tool);
      else if (!"analyzePoster".equals(name) && !"analyzeAudioExtract".equals(name))
        demanded.add(tool);
    }
    return demanded;
  }

  private String extractFullText(InternalChatRequest request, String promptText) {
    StringBuilder text = new StringBuilder(promptText != null ? promptText : "");
    text.append(" ").append(request.prompt() != null ? request.prompt() : "");
    if (request.messages() != null) {
      request.messages().stream()
          .filter(m -> m.content() != null)
          .forEach(m -> text.append(" ").append(m.content()));
    }
    return text.toString();
  }

  private boolean demandsPosterTool(String lowerContext) {
    return lowerContext.contains("poster")
        || lowerContext.contains("image")
        || lowerContext.contains("visual")
        || lowerContext.contains("picture");
  }

  private boolean demandsAudioTool(String lowerContext) {
    return lowerContext.contains("audio")
        || lowerContext.contains("clip")
        || lowerContext.contains("voice")
        || lowerContext.contains("music")
        || lowerContext.contains("sound");
  }

  private ChatOptions attachTools(ChatOptions options, List<ToolCallback> demandedTools) {
    Set<String> toolNames =
        demandedTools.stream().map(t -> t.getToolDefinition().name()).collect(Collectors.toSet());
    return switch (options) {
      case OllamaChatOptions ollama ->
          OllamaChatOptions.builder()
              .model(ollama.getModel())
              .temperature(ollama.getTemperature())
              .numPredict(ollama.getNumPredict())
              .toolNames(toolNames)
              .toolCallbacks(demandedTools)
              .build();
      case OpenAiChatOptions openai ->
          OpenAiChatOptions.builder()
              .model(openai.getModel())
              .temperature(openai.getTemperature())
              .maxTokens(openai.getMaxTokens())
              .toolNames(toolNames)
              .toolCallbacks(demandedTools)
              .build();
      default -> options;
    };
  }
}
