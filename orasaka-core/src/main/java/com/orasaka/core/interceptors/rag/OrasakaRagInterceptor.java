package com.orasaka.core.interceptors.rag;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.interceptors.OrasakaContextInterceptor;
import com.orasaka.core.model.OrasakaChatRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

@Component
public class OrasakaRagInterceptor implements OrasakaContextInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaRagInterceptor.class);

  private final CoreProperties properties;
  private final OrasakaKnowledgeService knowledgeService;

  public OrasakaRagInterceptor(
      CoreProperties properties, OrasakaKnowledgeService knowledgeService) {
    this.properties = properties;
    this.knowledgeService = knowledgeService;
  }

  @Override
  public ChatOptions preProcess(
      OrasakaChatRequest request, String promptText, List<Message> messages, ChatOptions options) {
    int ragContextSize = 0;
    if (properties.rag() != null && properties.rag().enabled() && knowledgeService != null) {
      String context = knowledgeService.retrieveContext(promptText, properties.rag().topK());
      if (context != null && !context.isBlank()) {
        messages.add(new SystemMessage("RAG Context: \n" + context));
        ragContextSize = context.length();
      }
    }
    logger.debug(
        "RAG Injection Context Size: {} characters (RAG enabled: {})",
        ragContextSize,
        (properties.rag() != null && properties.rag().enabled()));
    return options;
  }
}
