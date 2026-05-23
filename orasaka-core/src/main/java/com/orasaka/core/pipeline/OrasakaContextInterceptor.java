package com.orasaka.core.pipeline;

import com.orasaka.core.support.OrasakaChatRequest;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Generic interceptor interface for prompt context enrichment and post-execution hooks.
 *
 * <p>Enables Open/Closed extension of the cognitive core engine without coupling the core logic
 * directly to specialized domain components.
 */
public interface OrasakaContextInterceptor {

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
  ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options);

  /**
   * Invoked after the model execution completes successfully.
   *
   * @param request The original Orasaka chat request.
   * @param promptText The (possibly refined) prompt text that was sent.
   * @param responseText The text response returned by the model.
   */
  default void postProcess(OrasakaChatRequest request, String promptText, String responseText) {}
}
