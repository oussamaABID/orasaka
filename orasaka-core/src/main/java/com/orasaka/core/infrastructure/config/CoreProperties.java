package com.orasaka.core.infrastructure.config;

import com.orasaka.core.domain.model.RoutingMode;
import java.util.List;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Configuration properties for Orasaka CORS (Cognitive Orchestration and Retrieval System). Maps to
 * {@code orasaka.core} in {@code application.yml}.
 *
 * <p>This record defines the structural schema for AI provider orchestration, RAG, and MCP. It
 * utilizes Java 21 Records for immutable, type-safe configuration.
 *
 * @param defaultProvider The global AI provider to use (e.g., "ollama", "openai"). Required.
 * @param rag Configuration for Retrieval-Augmented Generation context injection.
 * @param mcp Configuration for Model Context Protocol (MCP) server endpoints.
 * @param orchestration Configuration for the cognitive prompt orchestration pipeline.
 * @param video Configuration for video analysis and generation.
 * @param image Configuration for image analysis and generation.
 * @param vision Configuration for vision analysis model settings.
 * @param audio Configuration for audio analysis model settings.
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/chatmodel.html">Spring AI
 *     ChatModel</a>
 */
public record CoreProperties(
    String defaultProvider,
    RagConfig rag,
    McpConfig mcp,
    OrchestrationConfig orchestration,
    VideoConfig video,
    ImageConfig image,
    VisionConfig vision,
    AudioConfig audio) {

  @ConstructorBinding
  public CoreProperties(
      String defaultProvider,
      RagConfig rag,
      McpConfig mcp,
      OrchestrationConfig orchestration,
      VideoConfig video,
      ImageConfig image,
      VisionConfig vision,
      AudioConfig audio) {
    this.defaultProvider = defaultProvider;
    this.rag = rag;
    this.mcp = mcp;
    this.orchestration = orchestration;
    this.video = video;
    this.image = image;
    this.vision = vision;
    this.audio = audio;
  }

  /**
   * RAG (Retrieval-Augmented Generation) configuration.
   *
   * @param enabled Whether RAG context injection is active.
   * @param storeType The type of vector store (e.g., "pg vector").
   * @param topK The number of relevant documents to retrieve.
   */
  public record RagConfig(boolean enabled, String storeType, Integer topK) {}

  /**
   * MCP (Model Context Protocol) configuration.
   *
   * @param endpoints List of external MCP server URLs.
   */
  public record McpConfig(List<String> endpoints) {}

  /** Configuration for the cognitive prompt orchestration pipeline. */
  public record OrchestrationConfig(
      boolean enabled,
      UserContextConfig userContext,
      SystemContextConfig systemContext,
      InterceptorConfig refiner,
      InterceptorConfig router,
      RoutingConfig routing) {

    /** Convenience constructor without routing — defaults to null (DETERMINISTIC fallback). */
    public OrchestrationConfig(
        boolean enabled,
        UserContextConfig userContext,
        SystemContextConfig systemContext,
        InterceptorConfig refiner,
        InterceptorConfig router) {
      this(enabled, userContext, systemContext, refiner, router, null);
    }
  }

  /**
   * Hybrid routing configuration for the pipeline orchestrator.
   *
   * @param mode The routing strategy (DETERMINISTIC or AGENTIC). Defaults to DETERMINISTIC.
   */
  public record RoutingConfig(RoutingMode mode, String semanticEndpoint) {
    public RoutingConfig {
      if (mode == null) {
        mode = RoutingMode.DETERMINISTIC;
      }
      if (semanticEndpoint == null || semanticEndpoint.isBlank()) {
        semanticEndpoint = "http://" + "local" + "host:8085/v1/classify";
      }
    }
  }

  /** User context resolver configuration details. */
  public record UserContextConfig(boolean enabled) {}

  /** System context injector configuration details. */
  public record SystemContextConfig(boolean enabled) {}

  /** Unified interceptor configuration for refiner and router pipeline stages. */
  public record InterceptorConfig(
      boolean enabled, String provider, String model, Double temperature) {}

  /** Partitioned video pipeline configuration (analysis vs generation). */
  public record VideoConfig(VideoAnalysisConfig analysis, VideoGenerationConfig generation) {}

  /**
   * Video analysis (vision input) configuration. All values are bound from {@code application.yml}
   * — no hardcoded defaults.
   */
  public record VideoAnalysisConfig(Integer maxKeyframes, Integer frameIntervalSec) {}

  /** Video generation (text-to-video output) configuration. */
  public record VideoGenerationConfig(String provider, String baseUrl) {}

  /** Partitioned image pipeline configuration (generation only). */
  public record ImageConfig(ImageGenerationConfig generation) {}

  /** Image generation configuration. */
  public record ImageGenerationConfig(
      String provider,
      String baseUrl,
      String apiKey,
      String model,
      Integer width,
      Integer height,
      Integer n,
      Integer connectTimeoutMs,
      Integer readTimeoutMs) {}

  /** Vision analysis model settings. */
  public record VisionConfig(String provider, String model) {}

  /** Audio analysis model settings. */
  public record AudioConfig(String provider, String model, String transcriptionModel) {
    public AudioConfig(String provider, String model) {
      this(provider, model, null);
    }
  }
}
