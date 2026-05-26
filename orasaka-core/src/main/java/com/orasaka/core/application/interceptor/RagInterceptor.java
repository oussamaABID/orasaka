package com.orasaka.core.application.interceptor;

import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.domain.ports.outbound.KnowledgeService;
import com.orasaka.core.infrastructure.config.CoreProperties;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

/**
 * RAG (Retrieval-Augmented Generation) interceptor — enriches the prompt with relevant knowledge
 * base context retrieved via {@link KnowledgeService}.
 *
 * <p>Conditional on {@code orasaka.core.rag.enabled} being {@code true}.
 *
 * @see KnowledgeService#retrieveContext(String, int)
 */
@Component
class RagInterceptor implements PromptContextInterceptor {
  private final CoreProperties properties;
  private final KnowledgeService knowledgeService;

  public RagInterceptor(CoreProperties properties, KnowledgeService knowledgeService) {
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
    this.knowledgeService =
        Objects.requireNonNull(knowledgeService, "KnowledgeService must not be null");
  }

  @Override
  public ChatOptions preProcess(
      InternalChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    Optional.ofNullable(properties.rag())
        .filter(CoreProperties.RagConfig::enabled)
        .ifPresent(
            rag -> {
              String context = knowledgeService.retrieveContext(promptText, rag.topK());
              if (context != null && !context.isBlank())
                messages.add(0, new SystemMessage("RAG Context: \n" + context));
            });
    return options;
  }
}
