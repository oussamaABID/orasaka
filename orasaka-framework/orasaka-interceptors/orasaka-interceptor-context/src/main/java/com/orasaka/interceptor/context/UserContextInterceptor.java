package com.orasaka.interceptor.context;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.infrastructure.config.CoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * ContextInterceptor that dynamically injects onboarding profile details (ai_behavior,
 * primary_industry) as structural system prompt constraints right before executing model inference
 * or streaming.
 */
class UserContextInterceptor implements PromptContextInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(UserContextInterceptor.class);

  private final CoreProperties properties;

  UserContextInterceptor(CoreProperties properties) {
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
  }

  @Override
  public ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {

    if (!isUserContextEnabled()) {
      return options;
    }

    try {
      Map<String, Object> preferences = request.context().preferences();
      String constraints = buildConstraints(preferences);
      if (!constraints.isEmpty()) {
        messages.add(0, new SystemMessage("System constraints:\n" + constraints));
        logger.debug("Successfully injected user profile system prompt constraints.");
      }
    } catch (RuntimeException e) {
      logger.error("Failed to inject user profile constraints in preProcess", e);
    }
    return options;
  }

  private boolean isUserContextEnabled() {
    return properties.orchestration() != null
        && properties.orchestration().userContext() != null
        && properties.orchestration().userContext().enabled();
  }

  private String buildConstraints(Map<String, Object> preferences) {
    StringBuilder constraints = new StringBuilder();
    String primaryIndustry = resolvePreference(preferences, "primary_industry", "primaryIndustry");
    String aiBehavior = resolvePreference(preferences, "ai_behavior", "aiBehavior");

    if (primaryIndustry != null && !primaryIndustry.isBlank()) {
      constraints.append("User Primary Industry: ").append(primaryIndustry).append("\n");
    }
    if (aiBehavior != null && !aiBehavior.isBlank()) {
      constraints.append("User AI Behavior: ").append(aiBehavior).append("\n");
    }
    return constraints.toString();
  }

  private String resolvePreference(Map<String, Object> prefs, String snakeKey, String camelKey) {
    String value = (String) prefs.get(snakeKey);
    return value != null ? value : (String) prefs.get(camelKey);
  }
}
