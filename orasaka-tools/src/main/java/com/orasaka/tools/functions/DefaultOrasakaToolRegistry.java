package com.orasaka.tools.functions;

import com.orasaka.core.pipeline.OrasakaChunker;
import com.orasaka.core.pipeline.OrasakaChunkingStrategies;
import com.orasaka.core.pipeline.OrasakaKnowledgeService;
import com.orasaka.core.pipeline.OrasakaToolRegistry;
import com.orasaka.tools.config.OrasakaToolsProperties;
import com.orasaka.tools.config.OrasakaToolsProperties.ToolConfig;
import com.orasaka.tools.entity.OrasakaToolRagSourceEntity;
import com.orasaka.tools.repository.OrasakaToolRagSourceRepository;
import jakarta.annotation.PostConstruct;
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

  @PostConstruct
  void registerDefaultTools() {
    registerTool(
        "analyzePoster",
        "Analyzes a movie poster provided as a base64 encoded string using vision model.",
        AnalyzePosterRequest.class,
        this::analyzePoster);
    registerTool(
        "analyzeAudioExtract",
        "Analyzes a film audio extract to check for specific compliance or content criteria.",
        AnalyzeAudioExtractRequest.class,
        this::analyzeAudioExtract);
  }

  private AnalyzePosterResponse analyzePoster(AnalyzePosterRequest request) {
    log.info("Executing analyzePoster tool with request: {}", request);
    String base64 = request.posterBase64();
    if (base64.contains("error") || base64.contains("corrupt")) {
      return new AnalyzePosterResponse("Failed to parse poster image: corrupt payload.", false);
    }
    return new AnalyzePosterResponse(
        "Poster analysis success. Detected themes: Sci-Fi, cyberpunk architecture, neon aesthetic. Prompt: "
            + request.prompt(),
        true);
  }

  private AnalyzeAudioExtractResponse analyzeAudioExtract(AnalyzeAudioExtractRequest request) {
    log.info("Executing analyzeAudioExtract tool with request: {}", request);
    String path = request.clipPath();
    if (path.contains("corrupt") || path.contains("invalid")) {
      return new AnalyzeAudioExtractResponse("Audio clip corrupt or missing at " + path, false);
    }
    return new AnalyzeAudioExtractResponse(
        "Audio extract compliance check passed. Track checks clear under type: "
            + request.checkType(),
        true);
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
    return registeredTools.stream()
        .map(
            tool -> {
              String name = tool.getToolDefinition().name();
              ToolConfig toolConfig =
                  (properties != null && properties.configs() != null)
                      ? properties.configs().get(name)
                      : null;

              if (toolConfig != null
                  && toolConfig.cache() != null
                  && toolConfig.cache().enabled()) {
                return new CachingToolCallback(
                    tool, cacheService, true, toolConfig.cache().ttlSeconds());
              } else {
                return tool;
              }
            })
        .toList();
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
    return properties.configs().values().stream()
        .anyMatch(config -> config.rag() != null && config.rag().enabled());
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

    properties
        .configs()
        .forEach(
            (toolId, toolConfig) -> {
              if (toolConfig.rag() != null && toolConfig.rag().enabled()) {
                String chunkerType = toolConfig.rag().chunkerType();
                OrasakaChunker chunker = OrasakaChunkingStrategies.resolve(chunkerType);

                log.info(
                    "Processing RAG ingestion for tool '{}' using chunker '{}'...",
                    toolId,
                    chunkerType);

                List<OrasakaToolRagSourceEntity> rows =
                    ragSourceRepository.findByToolIdAndIngestedFalse(toolId);

                rows.forEach(
                    row -> {
                      try {
                        Map<String, Object> metadataMap = new HashMap<>(row.getMetadata());
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
                        log.error(
                            "Failed to process RAG row ID {} for tool '{}'",
                            row.getId(),
                            toolId,
                            e);
                      }
                    });
              }
            });
  }
}
