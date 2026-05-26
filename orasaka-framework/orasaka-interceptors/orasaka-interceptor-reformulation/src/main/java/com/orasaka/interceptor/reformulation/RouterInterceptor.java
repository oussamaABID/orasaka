package com.orasaka.interceptor.reformulation;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.application.pipeline.PipelineOptionsRegistry;
import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.infrastructure.config.CoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

/**
 * Order 7 — Analyzes user intent and selects the optimal AI provider using an LLM classification
 * call with temperature 0.0 for determinism.
 *
 * <p>This interceptor is <strong>AI-dependent</strong> — it invokes an LLM model to determine
 * routing. The security governance kill-switch will block this interceptor when active.
 *
 * @since 1.0.0
 */
public class RouterInterceptor implements PromptContextInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(RouterInterceptor.class);

  private final Map<String, ChatModel> chatModels;
  private final CoreProperties properties;
  private final PipelineOptionsRegistry optionsRegistry;
  private final Resource routerSystemResource;
  private final Resource routerUserResource;

  public RouterInterceptor(
      Map<String, ChatModel> chatModels,
      CoreProperties properties,
      PipelineOptionsRegistry optionsRegistry,
      @Value("classpath:/prompts/router-system.st") Resource routerSystemResource,
      @Value("classpath:/prompts/router-user.st") Resource routerUserResource) {
    this.chatModels = Map.copyOf(Objects.requireNonNullElse(chatModels, Map.of()));
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
    this.optionsRegistry =
        Objects.requireNonNull(optionsRegistry, "PipelineOptionsRegistry must not be null");
    this.routerSystemResource =
        Objects.requireNonNull(routerSystemResource, "routerSystemResource must not be null");
    this.routerUserResource =
        Objects.requireNonNull(routerUserResource, "routerUserResource must not be null");
  }

  @Override
  public boolean isAiDependent() {
    return true;
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    if (!isRouterEnabled()) return context;

    String provider = resolveProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      model =
          chatModels.entrySet().stream()
              .filter(entry -> entry.getKey().toLowerCase().contains(provider.toLowerCase()))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElse(null);
    }
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
    String availableModels = String.join(", ", chatModels.keySet().stream().sorted().toList());
    var sysMsg = new SystemPromptTemplate(routerSystemResource).createMessage();
    var userMsg =
        new PromptTemplate(routerUserResource)
            .createMessage(
                Map.of(
                    "refinedPrompt", context.refinedPrompt(), "availableModels", availableModels));

    var opt = properties.orchestration().router();
    ChatOptions options = optionsRegistry.build(provider, opt.model(), 0.0);
    return new Prompt(List.of(sysMsg, userMsg), options);
  }

  private PromptContext executeRouting(ChatModel model, Prompt prompt, PromptContext context) {
    try {
      return ReformulationUtils.extractResponseText(model.call(prompt))
          .filter(chatModels::containsKey)
          .map(
              routedModel -> {
                logger.debug("Routed to: {}", routedModel);
                return context.withRoutedProvider(routedModel);
              })
          .orElse(context);
    } catch (RuntimeException e) {
      logger.error("Failed to route prompt.", e);
      return context;
    }
  }
}
