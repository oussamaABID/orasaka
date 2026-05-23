package com.orasaka.tools.functions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.core.rag.OrasakaChunker;
import com.orasaka.core.rag.OrasakaChunkingStrategies;
import com.orasaka.core.rag.OrasakaKnowledgeService;
import com.orasaka.core.tool.OrasakaToolRegistry;
import com.orasaka.tools.config.OrasakaToolsProperties;
import com.orasaka.tools.config.OrasakaToolsProperties.ToolConfig;
import com.orasaka.tools.entity.OrasakaToolRagSourceEntity;
import com.orasaka.tools.repository.OrasakaToolRagSourceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/**
 * Default implementation of OrasakaToolRegistry. Registry for local Java methods to be used as
 * tools by the AI models.
 *
 * <p>All database operations on RAG knowledge sources are executed using Spring Data JPA
 * repositories to eliminate raw SQL queries and uphold codebase standards (AGENTS.md §2.A).
 */
@Component
public class DefaultOrasakaToolRegistry implements OrasakaToolRegistry {

  private static final Logger log = LoggerFactory.getLogger(DefaultOrasakaToolRegistry.class);

  private final OrasakaToolsProperties properties;
  private final ToolCacheService cacheService;
  private final OrasakaToolRagSourceRepository ragSourceRepository;
  private final OrasakaKnowledgeService knowledgeService;
  private final List<ToolCallback> registeredTools = new ArrayList<>();

  /** Default no-args constructor for unit tests or fallback initialization. */
  public DefaultOrasakaToolRegistry() {
    this.properties = null;
    this.cacheService = null;
    this.ragSourceRepository = null;
    this.knowledgeService = null;
  }

  /**
   * Initializes the registry with tools, caching, and RAG dependencies.
   *
   * @param properties Active tools configurations block.
   * @param cacheService Multi-tier cache service.
   * @param ragSourceRepository Database repository for RAG knowledge sources.
   * @param knowledgeService RAG vector knowledge base abstraction.
   */
  public DefaultOrasakaToolRegistry(
      OrasakaToolsProperties properties,
      ToolCacheService cacheService,
      OrasakaToolRagSourceRepository ragSourceRepository,
      OrasakaKnowledgeService knowledgeService) {
    this.properties = properties;
    this.cacheService = cacheService;
    this.ragSourceRepository = ragSourceRepository;
    this.knowledgeService = knowledgeService;
  }

  /**
   * Registers a new Java function callback tool in the local registry.
   *
   * @param <I> The input argument type for the tool.
   * @param <O> The output result type from the tool.
   * @param name The name identifier of the tool.
   * @param description A descriptive string explaining what the tool does.
   * @param inputType The Class type representing the input.
   * @param function The functional logic representing the tool execution.
   */
  @Override
  public <I, O> void registerTool(
      String name, String description, Class<I> inputType, Function<I, O> function) {
    this.registeredTools.add(
        FunctionToolCallback.builder(name, function)
            .description(description)
            .inputType(inputType)
            .build());
  }

  /**
   * Retrieves all registered tool callbacks, wrapping any that have active caching properties
   * configured.
   *
   * @return An immutable List of wrapped or direct ToolCallback objects.
   */
  @Override
  public List<ToolCallback> getRegisteredTools() {
    List<ToolCallback> decorated = new ArrayList<>();
    for (ToolCallback tool : registeredTools) {
      String name = tool.getToolDefinition().name();
      ToolConfig toolConfig =
          (properties != null && properties.configs() != null)
              ? properties.configs().get(name)
              : null;

      if (toolConfig != null && toolConfig.cache() != null && toolConfig.cache().enabled()) {
        decorated.add(
            new CachingToolCallback(tool, cacheService, true, toolConfig.cache().ttlSeconds()));
      } else {
        // Bypass decorator entirely when cache config is omitted or disabled
        decorated.add(tool);
      }
    }
    return List.copyOf(decorated);
  }

  /**
   * Checks if RAG ingestion is enabled for any configured tool inside properties.
   *
   * @return True if at least one tool has RAG ingestion enabled, false otherwise.
   */
  @Override
  public boolean isRagIngestionEnabled() {
    if (properties == null || properties.configs() == null) {
      return false;
    }
    for (ToolConfig config : properties.configs().values()) {
      if (config.rag() != null && config.rag().enabled()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Scans for non-ingested RAG source database records, parses their contents, generates vector
   * embeddings through the knowledge service, and saves them.
   */
  @Override
  public void triggerIngestion() {
    if (knowledgeService == null || knowledgeService.getVectorStore() == null) {
      log.warn("VectorStore is not available. Skipping RAG ingestion.");
      return;
    }

    if (properties == null || properties.configs() == null || ragSourceRepository == null) {
      return;
    }

    for (Map.Entry<String, ToolConfig> entry : properties.configs().entrySet()) {
      String toolId = entry.getKey();
      ToolConfig toolConfig = entry.getValue();

      if (toolConfig.rag() != null && toolConfig.rag().enabled()) {
        String chunkerType = toolConfig.rag().chunkerType();
        OrasakaChunker chunker = OrasakaChunkingStrategies.resolve(chunkerType);

        log.info(
            "Processing RAG ingestion for tool '{}' using chunker '{}'...", toolId, chunkerType);

        List<OrasakaToolRagSourceEntity> rows =
            ragSourceRepository.findByToolIdAndIngestedFalse(toolId);

        for (OrasakaToolRagSourceEntity row : rows) {
          try {
            Map<String, Object> metadataMap = parseMetadataJson(row.getMetadata());
            metadataMap.put("tool_id", toolId);

            List<Document> chunks = chunker.chunk(row.getContent(), metadataMap);
            if (!chunks.isEmpty()) {
              knowledgeService.getVectorStore().add(chunks);
              log.debug(
                  "Ingested {} chunks for tool '{}', source row ID {}",
                  chunks.size(),
                  toolId,
                  row.getId());
            }

            row.setIngested(true);
            ragSourceRepository.save(row);
          } catch (Exception e) {
            log.error("Failed to process RAG row ID {} for tool '{}'", row.getId(), toolId, e);
          }
        }
      }
    }
  }

  /**
   * Parses RAG metadata JSON string.
   *
   * @param json The RAG metadata JSON string.
   * @return The parsed RAG metadata.
   */
  private Map<String, Object> parseMetadataJson(String json) {
    if (json == null || json.isBlank()) {
      return new HashMap<>();
    }
    try {
      return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.error("Failed to parse RAG metadata JSON string: {}", json, e);
      return new HashMap<>();
    }
  }
}
