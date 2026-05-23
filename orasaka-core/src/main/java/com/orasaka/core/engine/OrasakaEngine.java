package com.orasaka.core.engine;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.mcp.McpOrchestrator;
import com.orasaka.core.rag.OrasakaKnowledgeService;
import com.orasaka.core.tool.OrasakaToolRegistry;
import java.util.Map;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;

/**
 * Concrete, final implementation of {@link AbstractOrasakaEngine}.
 *
 * <p>This is the sole permitted subclass of the sealed {@link AbstractOrasakaEngine} hierarchy. It
 * delegates all orchestration logic to the abstract parent and serves as the default production
 * engine registered in {@link com.orasaka.core.config.OrasakaCoreConfiguration}.
 *
 * <p>All AI inference tasks dispatched through this engine are non-blocking — they are executed on
 * Java 21 Virtual Threads via the executor declared in the parent class.
 *
 * @see AbstractOrasakaEngine
 * @see com.orasaka.core.client.OrasakaAiClient
 * @see com.orasaka.core.config.OrasakaCoreConfiguration
 */
public final class OrasakaEngine extends AbstractOrasakaEngine {

  /**
   * Concrete constructor for OrasakaEngine.
   *
   * @param chatModels Map of chat models.
   * @param imageModels Map of image models.
   * @param embeddingModels Map of embedding models.
   * @param speechModels Map of speech models.
   * @param properties Configuration properties.
   * @param toolRegistry Registry for Java tools.
   * @param knowledgeService RAG service.
   * @param mcpOrchestrator MCP orchestrator.
   * @param memoryResolver Session-based memory resolver.
   * @param orchestrationPipeline Orchestration pipeline.
   */
  /**
   * Concrete constructor for OrasakaEngine.
   *
   * @param chatModels Map of chat models.
   * @param imageModels Map of image models.
   * @param embeddingModels Map of embedding models.
   * @param speechModels Map of speech models.
   * @param properties Configuration properties.
   * @param toolRegistry Registry for Java tools.
   * @param knowledgeService RAG service.
   * @param mcpOrchestrator MCP orchestrator.
   * @param memoryResolver Session-based memory resolver.
   * @param orchestrationPipeline Orchestration pipeline.
   */
  public OrasakaEngine(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties,
      OrasakaToolRegistry toolRegistry,
      OrasakaKnowledgeService knowledgeService,
      McpOrchestrator mcpOrchestrator,
      OrasakaMemoryResolver memoryResolver,
      com.orasaka.core.orchestration.OrasakaOrchestrationPipeline orchestrationPipeline) {
    super(
        chatModels,
        imageModels,
        embeddingModels,
        speechModels,
        properties,
        toolRegistry,
        knowledgeService,
        mcpOrchestrator,
        memoryResolver,
        orchestrationPipeline);
  }

  /** Backward-compatible constructor for OrasakaEngine. */
  public OrasakaEngine(
      Map<String, ChatModel> chatModels,
      Map<String, ImageModel> imageModels,
      Map<String, EmbeddingModel> embeddingModels,
      Map<String, TextToSpeechModel> speechModels,
      CoreProperties properties,
      OrasakaToolRegistry toolRegistry,
      OrasakaKnowledgeService knowledgeService,
      McpOrchestrator mcpOrchestrator,
      OrasakaMemoryResolver memoryResolver) {
    this(
        chatModels,
        imageModels,
        embeddingModels,
        speechModels,
        properties,
        toolRegistry,
        knowledgeService,
        mcpOrchestrator,
        memoryResolver,
        null);
  }
}
