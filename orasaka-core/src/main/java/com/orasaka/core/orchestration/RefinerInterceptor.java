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
 * Step 3: Refiner Interceptor.
 *
 * <p>Enriches fuzzy prompts with the User and System matrix maps using a designated LLM model.
 */
@Component
public class RefinerInterceptor implements PromptInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(RefinerInterceptor.class);

  private final Map<String, ChatModel> chatModels;
  private final CoreProperties properties;

  /**
   * Constructs the interceptor.
   *
   * @param chatModels Available LLM provider mapping.
   * @param properties Configuration properties.
   */
  public RefinerInterceptor(Map<String, ChatModel> chatModels, CoreProperties properties) {
    this.chatModels = chatModels;
    this.properties = properties;
  }

  @Override
  public void intercept(PromptContext context) {
    var opt = properties.orchestration() != null ? properties.orchestration().refiner() : null;
    if (opt == null || !opt.enabled()) {
      logger.debug("Refiner interceptor is disabled. Skipping prompt refinement.");
      return;
    }

    String provider = opt.provider() != null ? opt.provider() : properties.defaultProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      logger.warn(
          "Refinement provider '{}' is not registered in chat models. Skipping refinement.",
          provider);
      return;
    }

    logger.info("Refining prompt via LLM provider: {}, model: {}", provider, opt.model());

    String systemInstruction =
        "You are a prompt refinement engine for the Orasaka System. "
            + "Your goal is to enrich the user's raw query by incorporating the provided user and system metadata, "
            + "generating a highly detailed, precise, and contextual instruction prompt for a downstream LLM. "
            + "Do NOT answer the query yourself. Output ONLY the refined prompt, with no intro, markdown formatting, or wrap-up text.";

    String contextMatrix =
        String.format(
            "=== USER METADATA ===\n%s\n\n=== SYSTEM METADATA ===\n%s\n\n=== RAW QUERY ===\n%s",
            context.userMetadata().toString(),
            context.systemMetadata().toString(),
            context.rawUserQuery());

    ChatOptions options = buildOptions(provider, opt.model(), opt.temperature());
    Prompt prompt =
        new Prompt(
            List.of(new SystemMessage(systemInstruction), new UserMessage(contextMatrix)), options);

    try {
      ChatResponse response = model.call(prompt);
      if (response != null
          && response.getResult() != null
          && response.getResult().getOutput() != null) {
        String refined = response.getResult().getOutput().getText().strip();
        if (!refined.isBlank()) {
          logger.debug("Refinement generated: {}", refined);
          context.setRefinedPrompt(refined);
        }
      }
    } catch (Exception e) {
      logger.error("Failed to refine prompt using model, falling back to raw query.", e);
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
    return 3;
  }
}
