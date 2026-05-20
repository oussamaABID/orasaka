package com.orasaka.core.engine;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.rag.OrasakaKnowledgeService;
import com.orasaka.core.tool.OrasakaToolRegistry;
import com.orasaka.core.mcp.McpOrchestrator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;

import java.util.Map;

/**
 * Concrete implementation of AbstractOrasakaEngine.
 */
public class OrasakaEngine extends AbstractOrasakaEngine {

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
            OrasakaMemoryResolver memoryResolver) {
        super(chatModels, imageModels, embeddingModels, speechModels,
              properties, toolRegistry, knowledgeService, mcpOrchestrator, memoryResolver);
    }
}
