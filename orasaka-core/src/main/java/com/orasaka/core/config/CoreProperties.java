package com.orasaka.core.config;

import java.util.List;
import java.util.Map;

/**
 * Configuration properties for Orasaka CORS (Cognitive Orchestration & Retrieval System).
 * Maps to {@code orasaka.core} in {@code application.yml}.
 *
 * <p>This record defines the structural schema for AI provider orchestration, RAG, and MCP.
 * It utilizes Java 21 Records for immutable, type-safe configuration.
 *
 * @param defaultProvider The global AI provider to use (e.g., "ollama", "openai"). Required.
 * @param overrides A map of {@link ProviderConfig} for specific provider settings.
 * @param rag Configuration for Retrieval-Augmented Generation context injection.
 * @param mcp Configuration for Model Context Protocol (MCP) server endpoints.
 * 
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/chatmodel.html">Spring AI ChatModel</a>
 */
public record CoreProperties(
    String defaultProvider,
    Map<String, ProviderConfig> overrides,
    RagConfig rag,
    McpConfig mcp
) {
    /**
     * Configuration for a specific AI provider.
     *
     * @param model The specific LLM model name (e.g., "llama3", "gpt-4o").
     * @param baseUrl The endpoint URL for the provider (e.g., "http://localhost:11434").
     * @param temperature Sampling temperature (0.0 to 1.0).
     * @param maxTokens Maximum number of tokens to generate.
     * @param extra Provider-specific additional parameters.
     */
    public record ProviderConfig(
        String model,
        String baseUrl,
        Double temperature,
        Integer maxTokens,
        Map<String, Object> extra
    ) {}

    /**
     * RAG (Retrieval-Augmented Generation) configuration.
     *
     * @param enabled Whether RAG context injection is active.
     * @param storeType The type of vector store (e.g., "pg vector").
     * @param topK The number of relevant documents to retrieve.
     */
    public record RagConfig(
        boolean enabled,
        String storeType,
        Integer topK
    ) {}

    /**
     * MCP (Model Context Protocol) configuration.
     *
     * @param endpoints List of external MCP server URLs.
     */
    public record McpConfig(
        List<String> endpoints
    ) {}

}
