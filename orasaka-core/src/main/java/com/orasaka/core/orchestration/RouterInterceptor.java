package com.orasaka.core.orchestration;

import com.orasaka.core.config.CoreProperties;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * Step 4: Router Interceptor.
 *
 * <p>Evaluates prompt routing using a specialized router LLM model at temperature 0.0, selecting
 * the target model provider.
 */
@Component
public class RouterInterceptor implements PromptInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(RouterInterceptor.class);

  private final Map<String, ChatModel> chatModels;
  private final CoreProperties properties;

  /**
   * Constructs the router interceptor.
   *
   * @param chatModels Available LLM provider mapping.
   * @param properties Configuration properties.
   */
  public RouterInterceptor(Map<String, ChatModel> chatModels, CoreProperties properties) {
    this.chatModels = chatModels;
    this.properties = properties;
  }

  @Override
  public void intercept(PromptContext context) {
    var opt = properties.orchestration() != null ? properties.orchestration().router() : null;
    if (opt == null || !opt.enabled()) {
      logger.debug("Router interceptor is disabled. Using default provider.");
      return;
    }

    String provider = opt.provider() != null ? opt.provider() : properties.defaultProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      logger.warn(
          "Router provider '{}' is not registered in chat models. Skipping routing.", provider);
      return;
    }

    logger.info("Routing prompt via LLM provider: {}, model: {}", provider, opt.model());

    String systemInstruction =
        "You are a prompt router for the Orasaka System. "
            + "Evaluate the user's refined query and select the most specialized LLM execution provider. "
            + "Available providers are: 'ollama' (for generic, local, lightweight operations) "
            + "and 'openai' (for high-fidelity, complex logic, code generation, or translation). "
            + "Output exactly one word representing the provider key: either 'ollama' or 'openai'. "
            + "Do NOT write any additional text, explanation, or markdown wrappers.";

    ChatOptions options = buildOptions(provider, opt.model(), opt.temperature());
    Prompt prompt =
        new Prompt(
            List.of(new SystemMessage(systemInstruction), new UserMessage(context.refinedPrompt())),
            options);

    try {
      ChatResponse response = model.call(prompt);
      if (response != null
          && response.getResult() != null
          && response.getResult().getOutput() != null) {
        String routed = response.getResult().getOutput().getText().strip().toLowerCase();
        if (chatModels.containsKey(routed)) {
          logger.info("Pipeline routed request to target provider: '{}'", routed);
          context.setRoutedProvider(routed);
        } else {
          logger.warn(
              "Pipeline routed request to unregistered provider key: '{}'. Ignoring selection.",
              routed);
        }
      }
    } catch (Exception e) {
      logger.error(
          "Failed to dynamically route prompt using model, falling back to default provider.", e);
    }
  }

  private ChatOptions buildOptions(String provider, String modelName, Double temp) {
    return switch (provider.toLowerCase()) {
      case "ollama" -> OllamaChatOptions.builder().model(modelName).temperature(temp).build();
      case "openai" -> OpenAiChatOptions.builder().model(modelName).temperature(temp).build();
      default -> null;
    };
  }

  @Override
  public int getOrder() {
    return 4;
  }
}
