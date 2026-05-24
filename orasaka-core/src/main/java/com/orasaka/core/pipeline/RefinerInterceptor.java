package com.orasaka.core.pipeline;

import com.orasaka.core.engine.CoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Order 3 — Resolves fuzzy user questions against compiled context matrices and reformulates them
 * into clear, explicit instructions using an LLM call.
 *
 * <p>Uses the {@code system-refinement.st} and {@code context-envelope.st} prompt templates to
 * construct a refinement prompt with full user + system metadata.
 *
 * @see PipelineInterceptors#extractResponseText(ChatResponse)
 * @since 1.0.0
 */
@Component
class RefinerInterceptor implements PromptInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(RefinerInterceptor.class);

  private final Map<String, ChatModel> chatModels;
  private final CoreProperties properties;

  @Value("classpath:/prompts/system-refinement.st")
  private Resource systemRefinementResource;

  @Value("classpath:/prompts/context-envelope.st")
  private Resource contextEnvelopeResource;

  public RefinerInterceptor(Map<String, ChatModel> chatModels, CoreProperties properties) {
    this.chatModels = Map.copyOf(Objects.requireNonNullElse(chatModels, Map.of()));
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    if (!isRefinementEnabled()) return context;

    String provider = resolveProvider();
    ChatModel model = chatModels.get(provider);
    if (model == null) {
      logger.warn("Refinement provider '{}' is not registered.", provider);
      return context;
    }

    Prompt prompt = buildRefinementPrompt(provider, context);
    return executeRefinement(model, prompt, context);
  }

  private boolean isRefinementEnabled() {
    return Optional.ofNullable(properties.orchestration())
        .map(CoreProperties.OrchestrationConfig::refiner)
        .map(CoreProperties.InterceptorConfig::enabled)
        .orElse(false);
  }

  private String resolveProvider() {
    return Optional.ofNullable(properties.orchestration())
        .map(CoreProperties.OrchestrationConfig::refiner)
        .map(CoreProperties.InterceptorConfig::provider)
        .orElse(properties.defaultProvider());
  }

  private Prompt buildRefinementPrompt(String provider, PromptContext context) {
    var sysMsg = new SystemPromptTemplate(systemRefinementResource).createMessage();
    var userMsg =
        new PromptTemplate(contextEnvelopeResource)
            .createMessage(
                Map.of(
                    "userMetadata", context.userMetadata().toString(),
                    "systemMetadata", context.systemMetadata().toString(),
                    "rawQuery", context.rawUserQuery()));

    var opt = properties.orchestration().refiner();
    ChatOptions options = PipelineOptionsBuilder.build(provider, opt.model(), opt.temperature());
    return new Prompt(List.of(sysMsg, userMsg), options);
  }

  private PromptContext executeRefinement(ChatModel model, Prompt prompt, PromptContext context) {
    try {
      return PipelineInterceptors.extractResponseText(model.call(prompt))
          .filter(refined -> !refined.isBlank())
          .map(
              refined -> {
                logger.debug("Refinement generated: {}", refined);
                return context.withRefinedPrompt(refined);
              })
          .orElse(context);
    } catch (Exception e) {
      logger.error("Failed to refine prompt.", e);
      return context;
    }
  }

  @Override
  public int getOrder() {
    return 3;
  }
}
