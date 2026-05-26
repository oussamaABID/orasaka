package com.orasaka.core.application.interceptor;

import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.domain.model.chat.InternalChatRequest;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Core port interface for the prompt and context orchestration pipeline. Unifies PromptInterceptor
 * and ContextInterceptor capabilities.
 *
 * <p>Implementations are loaded dynamically via Spring Boot auto-configuration from external
 * interceptor submodules ({@code orasaka-interceptor-*}).
 *
 * <p>Interceptors that require AI model calls must override {@link #isAiDependent()} to return
 * {@code true}. When the governance kill-switch ({@code orasaka.security.disable-ai=true}) is
 * active, the {@code DynamicPipelineExecutor} will throw a hard {@link SecurityException} if any
 * AI-dependent interceptor is invoked.
 */
public interface PromptContextInterceptor {

  /**
   * Intercepts and enriches the PromptContext.
   *
   * @param context The current prompt context state.
   * @return The updated prompt context state.
   */
  default PromptContext intercept(PromptContext context) {
    return context;
  }

  /**
   * Declares the precedence execution order for the interceptor (lower value executes first).
   *
   * @return Execution order index.
   */
  default int getOrder() {
    return 0;
  }

  /**
   * Indicates whether this interceptor depends on AI model inference calls. Interceptors returning
   * {@code true} will be blocked by the security governance kill-switch when {@code
   * orasaka.security.disable-ai=true} is set.
   *
   * @return {@code true} if this interceptor requires AI model calls, {@code false} otherwise.
   */
  default boolean isAiDependent() {
    return false;
  }

  /**
   * Enriches request messages or ChatOptions before sending to the Spring AI models.
   *
   * @param request The original Orasaka chat request.
   * @param promptText The current (possibly refined) prompt text.
   * @param messages The accumulator list of mapped messages.
   * @param options The current Spring AI chat options.
   * @return The updated ChatOptions (must not return null; should return the passed options if
   *     unchanged).
   */
  default ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    return options;
  }

  /**
   * Invoked after the model execution completes successfully.
   *
   * @param request The original Orasaka chat request.
   * @param promptText The (possibly refined) prompt text that was sent.
   * @param responseText The text response returned by the model.
   */
  default void postProcess(InternalChatRequest request, String promptText, String responseText) {}
}
