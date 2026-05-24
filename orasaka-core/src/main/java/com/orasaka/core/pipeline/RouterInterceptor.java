package com.orasaka.core.pipeline;

import com.orasaka.core.engine.CoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Order 4 — Evaluates input intent at {@code temperature: 0.0} to dynamically route the refined
 * request to the optimal model provider.
 *
 * <p>Uses the {@code system-router.st} prompt template to instruct the model to output only the
 * provider name. The result is validated against registered ChatModel keys.
 *
 * @see PipelineInterceptors#extractResponseText(ChatResponse)
 * @since 1.0.0
 */
@Component
class RouterInterceptor implements PromptInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(RouterInterceptor.class);

  private final Map<String, ChatModel> chatModels;
  private final CoreProperties properties;

  @Value("classpath:/prompts/system-router.st")
  private Resource systemRouterResource;

  public RouterInterceptor(Map<String, ChatModel> chatModels, CoreProperties properties) {
    this.chatModels = Map.copyOf(Objects.requireNonNullElse(chatModels, Map.of()));
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    if (!isRouterEnabled()) return context;

    String provider = resolveProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      logger.warn("Router provider '{}' is not registered.", provider);
      return context;
    }

    Prompt prompt = buildRouterPrompt(provider, context);
    return executeRouting(model, prompt, context);
  }

  private boolean isRouterEnabled() {
    return Optional.ofNullable(properties.orchestration())
        .map(CoreProperties.OrchestrationConfig::router)
        .map(CoreProperties.InterceptorConfig::enabled)
        .orElse(false);
  }

  private String resolveProvider() {
    return Optional.ofNullable(properties.orchestration())
        .map(CoreProperties.OrchestrationConfig::router)
        .map(CoreProperties.InterceptorConfig::provider)
        .orElse(properties.defaultProvider());
  }

  private Prompt buildRouterPrompt(String provider, PromptContext context) {
    var sysMsg = new SystemPromptTemplate(systemRouterResource).createMessage();
    var userMsg = new UserMessage(context.refinedPrompt());
    var opt = properties.orchestration().router();
    ChatOptions options = PipelineOptionsBuilder.build(provider, opt.model(), opt.temperature());
    return new Prompt(List.of(sysMsg, userMsg), options);
  }

  private PromptContext executeRouting(ChatModel model, Prompt prompt, PromptContext context) {
    try {
      return PipelineInterceptors.extractResponseText(model.call(prompt))
          .map(String::toLowerCase)
          .filter(chatModels::containsKey)
          .map(context::withRoutedProvider)
          .orElse(context);
    } catch (Exception e) {
      logger.error("Failed to dynamically route prompt.", e);
      return context;
    }
  }

  @Override
  public int getOrder() {
    return 4;
  }
}
