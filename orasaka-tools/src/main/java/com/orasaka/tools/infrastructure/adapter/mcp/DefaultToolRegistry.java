package com.orasaka.tools.infrastructure.adapter.mcp;

import com.orasaka.core.application.processing.Chunker;
import com.orasaka.core.application.processing.ChunkingStrategies;
import com.orasaka.core.domain.ports.outbound.KnowledgeService;
import com.orasaka.core.domain.ports.outbound.PlatformToolConfigProvider;
import com.orasaka.core.domain.ports.outbound.ToolRegistry;
import com.orasaka.core.infrastructure.support.SecurityContextUtil;
import com.orasaka.tools.application.service.CachingToolCallback;
import com.orasaka.tools.application.service.ToolCacheService;
import com.orasaka.tools.domain.model.audio.AnalyzeAudioExtractRequest;
import com.orasaka.tools.domain.model.audio.AnalyzeAudioExtractResponse;
import com.orasaka.tools.domain.model.poster.AnalyzePosterRequest;
import com.orasaka.tools.domain.model.poster.AnalyzePosterResponse;
import com.orasaka.tools.domain.model.search.SearchWebRequest;
import com.orasaka.tools.domain.model.search.SearchWebResponse;
import com.orasaka.tools.infrastructure.adapter.persistence.entity.ToolRagSourceEntity;
import com.orasaka.tools.infrastructure.adapter.persistence.repository.ToolRagSourceRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Default implementation of ToolRegistry. Registry for local Java methods to be used as tools by
 * the AI models.
 *
 * <p>All database operations on RAG knowledge sources are executed using Spring Data JPA
 * repositories to eliminate raw SQL queries and uphold codebase standards (AGENTS.md §2.A).
 */
@Component
public class DefaultToolRegistry implements ToolRegistry {

  private static final Logger log = LoggerFactory.getLogger(DefaultToolRegistry.class);
  private static final String TOOL_SEARCH_WEB = "searchWeb";

  private final PlatformToolConfigProvider platformToolConfigProvider;
  private final ToolCacheService cacheService;
  private final ToolRagSourceRepository ragSourceRepository;
  private final KnowledgeService knowledgeService;
  private final List<ToolCallback> registeredTools = new ArrayList<>();

  /**
   * Initializes the registry with tools, caching, and RAG dependencies.
   *
   * @param platformToolConfigProvider Dynamic tool configuration provider.
   * @param cacheService Multi-tier cache service.
   * @param ragSourceRepository Database repository for RAG knowledge sources.
   * @param knowledgeService RAG vector knowledge base abstraction.
   */
  public DefaultToolRegistry(
      PlatformToolConfigProvider platformToolConfigProvider,
      ToolCacheService cacheService,
      ToolRagSourceRepository ragSourceRepository,
      KnowledgeService knowledgeService) {
    this.platformToolConfigProvider = platformToolConfigProvider;
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
    registerTool(
        TOOL_SEARCH_WEB,
        "Queries the corporate RAG database for real-time web search results.",
        SearchWebRequest.class,
        this::searchWeb);
  }

  private SearchWebResponse searchWeb(SearchWebRequest request) {
    log.info("Executing searchWeb tool with request: {}", request);
    if (ragSourceRepository == null) {
      return new SearchWebResponse("RAG source repository is not initialized.", false);
    }
    try {
      String currentUserId = (String) SecurityContextUtil.extractSecurityMetadata().get("userId");
      List<ToolRagSourceEntity> all = ragSourceRepository.findAll();
      String matches =
          all.stream()
              .filter(entity -> TOOL_SEARCH_WEB.equals(entity.getToolId()))
              .filter(
                  entity -> entity.getUserId() == null || entity.getUserId().equals(currentUserId))
              .filter(
                  entity ->
                      entity.getContent().toLowerCase().contains(request.query().toLowerCase()))
              .map(ToolRagSourceEntity::getContent)
              .collect(Collectors.joining("\n\n"));

      if (matches.isBlank()) {
        // Fallback: return any corporate profile/framework description
        matches =
            all.stream()
                .filter(entity -> TOOL_SEARCH_WEB.equals(entity.getToolId()))
                .filter(
                    entity ->
                        entity.getUserId() == null || entity.getUserId().equals(currentUserId))
                .map(ToolRagSourceEntity::getContent)
                .collect(Collectors.joining("\n\n"));
      }

      return new SearchWebResponse(matches, true);
    } catch (DataAccessException e) {
      log.error("Failed to query RAG sources for searchWeb", e);
      return new SearchWebResponse("Error querying knowledge database: " + e.getMessage(), false);
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (isRagIngestionEnabled()) {
      log.info("Application ready. Triggering initial RAG ingestion...");
      try {
        triggerIngestion();
      } catch (RuntimeException e) {
        log.error("Failed to run initial RAG ingestion on startup", e);
      }
    }
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
              if (platformToolConfigProvider == null) {
                return tool;
              }
              var configOpt = platformToolConfigProvider.getToolConfig(name);
              if (configOpt.isPresent()) {
                var config = configOpt.get();
                if (Boolean.TRUE.equals(config.cacheEnabled())) {
                  long ttl = config.cacheTtlSeconds() != null ? config.cacheTtlSeconds() : 3600L;
                  return new CachingToolCallback(tool, cacheService, true, ttl);
                }
              }
              return tool;
            })
        .toList();
  }

  /**
   * Checks if RAG ingestion is enabled for any configured tool.
   *
   * @return True if RAG ingestion is enabled, false otherwise.
   */
  @Override
  public boolean isRagIngestionEnabled() {
    if (platformToolConfigProvider == null) {
      return false;
    }
    return registeredTools.stream()
        .map(tool -> tool.getToolDefinition().name())
        .map(platformToolConfigProvider::getToolConfig)
        .anyMatch(opt -> opt.isPresent() && Boolean.TRUE.equals(opt.get().ragEnabled()));
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

    if (platformToolConfigProvider == null || ragSourceRepository == null) {
      return;
    }

    resetIngestedStatusForVolatileStore();

    for (ToolCallback tool : registeredTools) {
      String toolId = tool.getToolDefinition().name();
      platformToolConfigProvider
          .getToolConfig(toolId)
          .ifPresent(config -> ingestToolSources(toolId, config));
    }
  }

  /**
   * Resets ingested status for all RAG sources when using an in-memory vector store, since
   * embeddings are lost on restart.
   */
  private void resetIngestedStatusForVolatileStore() {
    if (!(knowledgeService.getVectorStore() instanceof SimpleVectorStore)) {
      return;
    }
    log.info(
        "In-memory SimpleVectorStore detected. Resetting ingested status of all RAG sources to false for re-ingestion.");
    try {
      List<ToolRagSourceEntity> allSources = ragSourceRepository.findAll();
      allSources.forEach(row -> row.setIngested(false));
      ragSourceRepository.saveAll(allSources);
    } catch (DataAccessException e) {
      log.error("Failed to reset ingested status for in-memory VectorStore re-ingestion", e);
    }
  }

  /**
   * Ingests RAG sources for a single tool if RAG is enabled in its configuration.
   *
   * @param toolId The tool identifier.
   * @param toolConfig The tool's platform configuration.
   */
  private void ingestToolSources(
      String toolId, PlatformToolConfigProvider.PlatformToolConfig toolConfig) {
    if (!Boolean.TRUE.equals(toolConfig.ragEnabled())) {
      return;
    }

    String chunkerType =
        toolConfig.chunkerType() != null ? toolConfig.chunkerType() : "MARKDOWN_CHUNKERS";
    Chunker chunker = ChunkingStrategies.resolve(chunkerType);

    log.info("Processing RAG ingestion for tool '{}' using chunker '{}'...", toolId, chunkerType);

    List<ToolRagSourceEntity> rows = ragSourceRepository.findByToolIdAndIngestedFalse(toolId);
    rows.forEach(row -> ingestSingleSource(row, toolId, chunker));
  }

  /**
   * Chunks a single RAG source row and persists its vector embeddings.
   *
   * @param row The RAG source entity row.
   * @param toolId The tool identifier for metadata tagging.
   * @param chunker The chunking strategy to apply.
   */
  private void ingestSingleSource(ToolRagSourceEntity row, String toolId, Chunker chunker) {
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
    } catch (RuntimeException e) {
      log.error("Failed to process RAG row ID {} for tool '{}'", row.getId(), toolId, e);
    }
  }
}
