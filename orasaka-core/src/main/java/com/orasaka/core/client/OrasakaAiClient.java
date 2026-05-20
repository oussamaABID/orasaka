package com.orasaka.core.client;

import com.orasaka.core.engine.AbstractOrasakaEngine;
import com.orasaka.core.model.OrasakaChatRequest;
import com.orasaka.core.model.OrasakaChatResponse;
import com.orasaka.core.model.OrasakaImageRequest;
import com.orasaka.core.model.OrasakaImageResponse;
import com.orasaka.core.model.OrasakaSpeechRequest;
import com.orasaka.core.rag.OrasakaKnowledgeService;
import com.orasaka.core.tool.OrasakaToolRegistry;
import org.springframework.stereotype.Component;

/**
 * High-level Facade for developers to interact with the Orasaka AI Ecosystem.
 * 
 * <p>This client serves as the primary entry point for all AI interactions, including
 * multi-modal chat, image generation, and knowledge retrieval. It automatically 
 * orchestrates provider resolution, RAG context injection, and local tool execution 
 * through the underlying engine.
 * 
 * <p>Execution is handled via Virtual Threads in the engine layer to ensure 
 * optimal performance in high-concurrency environments.
 *
 * @see com.orasaka.core.engine.AbstractOrasakaEngine
 * @see com.orasaka.core.rag.OrasakaKnowledgeService
 * @see com.orasaka.core.tool.OrasakaToolRegistry
 */
@Component
public class OrasakaAiClient {

    private final AbstractOrasakaEngine engine;
    private final OrasakaToolRegistry toolRegistry;
    private final OrasakaKnowledgeService knowledgeService;

    /**
     * Initializes the facade with required orchestration services.
     *
     * @param engine The core AI orchestration engine.
     * @param toolRegistry The registry for local Java tools.
     * @param knowledgeService The RAG knowledge management service.
     */
    public OrasakaAiClient(
            AbstractOrasakaEngine engine,
            OrasakaToolRegistry toolRegistry,
            OrasakaKnowledgeService knowledgeService) {
        this.engine = engine;
        this.toolRegistry = toolRegistry;
        this.knowledgeService = knowledgeService;
    }

    /**
     * Executes a unified chat interaction with automatic RAG and Tooling support.
     * 
     * <p>This method leverages Virtual Threads via the engine to remain non-blocking.
     *
     * @param request The domain-specific chat request.
     * @return An {@link OrasakaChatResponse} containing the generated content and provider metadata.
     * @see OrasakaChatRequest
     */
    public OrasakaChatResponse chat(OrasakaChatRequest request) {
        return engine.chat(request);
    }

    /**
     * Triggers multi-modal image generation flows.
     * 
     * <p>This method leverages Virtual Threads via the engine to remain non-blocking.
     *
     * @param request The image generation specification.
     * @return An {@link OrasakaImageResponse} containing image metadata or URLs.
     */
    public OrasakaImageResponse generateImage(OrasakaImageRequest request) {
        return engine.generateImage(request);
    }

    /**
     * Triggers multi-modal speech generation (TTS) flows.
     * 
     * @param request The speech generation specification.
     * @return The audio metadata or stream info (provider dependent).
     */
    public byte[] generateSpeech(OrasakaSpeechRequest request) {
        return engine.generateSpeech(request);
    }

    /**
     * Provides access to the local tool registry for manual tool management.
     *
     * @return The active {@link OrasakaToolRegistry}.
     */
    public OrasakaToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    /**
     * Provides access to the knowledge service for RAG configuration.
     *
     * @return The active {@link OrasakaKnowledgeService}.
     */
    public OrasakaKnowledgeService getKnowledgeService() {
        return knowledgeService;
    }
}
