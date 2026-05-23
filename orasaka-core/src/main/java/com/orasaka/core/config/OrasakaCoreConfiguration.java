package com.orasaka.core.config;

import com.orasaka.core.engine.OrasakaEngine;
import com.orasaka.core.graph.OrasakaAdminRegistry;
import com.orasaka.core.graph.OrasakaFeaturesProperties;
import com.orasaka.core.graph.OrasakaGraphEngine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring {@link Configuration} that wires environment-driven AI provider models into the Orasaka
 * engine and registers all core service beans.
 *
 * <p>All model instances ({@link ChatModel}, {@link EmbeddingModel}, {@link ImageModel}, {@link
 * TextToSpeechModel}) are built dynamically from the {@link CoreProperties} record, which itself is
 * populated from {@code application.yml} via {@link Environment} bindings. No model name, API key,
 * or base URL is hardcoded — all are externalized (AGENTS.md §1.E).
 *
 * <p>Provider maps are keyed by the provider name string (e.g., {@code "ollama"}, {@code "openai"})
 * and passed to the {@link OrasakaEngine} constructor which selects the active provider at runtime
 * based on {@link CoreProperties#defaultProvider()}.
 *
 * @see CoreProperties
 * @see OrasakaEngine
 * @see com.orasaka.core.engine.AbstractOrasakaEngine
 */
@Configuration
public class OrasakaCoreConfiguration {

  /**
   * Declares the primary {@link OrasakaEngine} bean, injecting all resolved model maps and
   * orchestration services.
   *
   * <p>The engine is the central orchestrator for RAG, MCP, tool attachment, and AI inference. It
   * is exposed externally only through the {@link com.orasaka.core.client.OrasakaAiClient} facade
   * (Bridge Pattern 2.0 — AGENTS.md §1.B).
   *
   * @param properties Externalized configuration for provider selection and overrides.
   * @param interceptors Cognitive context interceptors chain.
   * @param chatModels Map of provider-key → {@link ChatModel} implementations.
   * @param imageModels Map of provider-key → {@link ImageModel} implementations.
   * @param embeddingModels Map of provider-key → {@link EmbeddingModel} implementations.
   * @param speechModels Map of provider-key → {@link TextToSpeechModel} implementations.
   * @return A fully initialized {@link OrasakaEngine} ready for Virtual Thread execution.
   * @see com.orasaka.core.client.OrasakaAiClient
   */

  /**
   * Builds the provider-keyed map of {@link ChatModel} instances from configuration.
   *
   * <p>Currently supported providers:
   *
   * <ul>
   *   <li><strong>ollama</strong> — configured via {@code orasaka.core.overrides.ollama}. Defaults
   *       to {@code http://localhost:11434} and model {@code llama3:8b} if not overridden.
   *   <li><strong>openai</strong> — configured via {@code orasaka.core.overrides.openai}. Requires
   *       {@code OPENAI_API_KEY} environment variable; falls back to a dummy key for local runs.
   * </ul>
   *
   * Providers not present in {@link CoreProperties#overrides()} are silently omitted from the map.
   *
   * @param properties The resolved core configuration record.
   * @return A {@link Map} of provider names to their {@link ChatModel} implementations.
   * @see CoreProperties.ProviderConfig
   */
  @Bean
  public Map<String, ChatModel> chatModels(CoreProperties properties) {
    Map<String, ChatModel> models = new HashMap<>();

    // 1. Ollama
    CoreProperties.ProviderConfig ollama = properties.overrides().get("ollama");
    if (ollama != null) {
      String baseUrl = ollama.baseUrl() != null ? ollama.baseUrl() : "http://localhost:11434";
      String model = ollama.model() != null ? ollama.model() : "llama3:8b";
      OllamaApi ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();

      OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder().model(model);
      if (ollama.temperature() != null) {
        optionsBuilder.temperature(ollama.temperature());
      }

      models.put(
          "ollama",
          OllamaChatModel.builder()
              .ollamaApi(ollamaApi)
              .defaultOptions(optionsBuilder.build())
              .build());
    }

    // 2. OpenAI
    CoreProperties.ProviderConfig openai = properties.overrides().get("openai");
    if (openai != null && openai.baseUrl() != null) {
      String apiKey = System.getenv("OPENAI_API_KEY");
      if (apiKey == null) {
        apiKey = "dummy-key";
      }
      OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).baseUrl(openai.baseUrl()).build();
      models.put("openai", OpenAiChatModel.builder().openAiApi(openAiApi).build());
    }

    return models;
  }

  /**
   * Builds the provider-keyed map of {@link EmbeddingModel} instances from configuration.
   *
   * <p>Currently, only the {@code ollama} provider is wired for embedding. The embedding model name
   * is resolved from {@code orasaka.core.overrides.ollama.extra.embedding-model}, defaulting to
   * {@code all-minilm} per the local resource constraints directive (AGENTS.md §1.D).
   *
   * @param properties The resolved core configuration record.
   * @return A {@link Map} of provider names to their {@link EmbeddingModel} implementations.
   * @see CoreProperties.ProviderConfig#extra()
   */
  @Bean
  public Map<String, EmbeddingModel> embeddingModels(CoreProperties properties) {
    Map<String, EmbeddingModel> models = new HashMap<>();

    // Ollama Embedding
    CoreProperties.ProviderConfig ollama = properties.overrides().get("ollama");
    if (ollama != null) {
      String baseUrl = ollama.baseUrl() != null ? ollama.baseUrl() : "http://localhost:11434";
      String model = "all-minilm";
      if (ollama.extra() != null && ollama.extra().containsKey("embedding-model")) {
        model = ollama.extra().get("embedding-model").toString();
      }
      OllamaApi ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();
      OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder().model(model).build();
      models.put(
          "ollama",
          OllamaEmbeddingModel.builder().ollamaApi(ollamaApi).defaultOptions(options).build());
    }

    return models;
  }

  /**
   * Builds the provider-keyed map of {@link ImageModel} instances from configuration.
   *
   * <p>Currently, only the {@code openai} provider supports image generation via Spring AI 1.1.6.
   * Ollama does not expose a dedicated image generation model in this version.
   *
   * @param properties The resolved core configuration record.
   * @return A {@link Map} of provider names to their {@link ImageModel} implementations.
   */
  @Bean
  public Map<String, ImageModel> imageModels(CoreProperties properties) {
    Map<String, ImageModel> models = new HashMap<>();

    // OpenAI Image
    CoreProperties.ProviderConfig openai = properties.overrides().get("openai");
    if (openai != null && openai.baseUrl() != null) {
      String apiKey = System.getenv("OPENAI_API_KEY");
      if (apiKey == null) {
        apiKey = "dummy-key";
      }
      OpenAiImageApi api =
          OpenAiImageApi.builder().apiKey(apiKey).baseUrl(openai.baseUrl()).build();
      models.put("openai", new OpenAiImageModel(api));
    }

    return models;
  }

  /**
   * Provides an empty {@link TextToSpeechModel} map as a placeholder.
   *
   * <p>TTS provider wiring is deferred until a supported Spring AI 1.1.6-compatible TTS
   * implementation is available. Voice preference resolution from {@link
   * com.orasaka.core.context.OrasakaContext} is already implemented in {@link
   * com.orasaka.core.engine.AbstractOrasakaEngine} and will activate automatically once a provider
   * is registered here.
   *
   * @return An empty mutable {@link Map}, to be populated by future provider beans.
   */
  @Bean
  public Map<String, TextToSpeechModel> speechModels() {
    return new HashMap<>();
  }

  /**
   * Constructs the {@link CoreProperties} record by binding values from the Spring {@link
   * Environment}.
   *
   * <p>This manual binding approach is required because {@code orasaka-core} must not declare
   * {@code spring-boot-starter} as a dependency (AGENTS.md §1.A). The {@link Environment} is
   * injected by the host application's Spring context and provides access to all {@code
   * application.yml} properties and environment variable overrides.
   *
   * <p>Property prefix: {@code orasaka.core.*}. All values support environment variable
   * substitution via the standard {@code ${ENV_VAR:default}} Spring syntax in the consuming
   * application's YAML.
   *
   * @param env The Spring {@link Environment} provided by the host application context.
   * @return A fully initialized, immutable {@link CoreProperties} record.
   * @see CoreProperties
   */
  @Bean
  public CoreProperties coreProperties(Environment env) {
    String defaultProvider = env.getProperty("orasaka.core.default-provider", "ollama");

    // RAG Config
    Boolean ragEnabled = env.getProperty("orasaka.core.rag.enabled", Boolean.class, false);
    String storeType = env.getProperty("orasaka.core.rag.store-type");
    Integer topK = env.getProperty("orasaka.core.rag.top-k", Integer.class, 3);
    CoreProperties.RagConfig rag = new CoreProperties.RagConfig(ragEnabled, storeType, topK);

    // MCP Config
    String endpointsStr = env.getProperty("orasaka.core.mcp.endpoints");
    List<String> endpoints = List.of();
    if (endpointsStr != null && !endpointsStr.isBlank()) {
      endpoints = List.of(endpointsStr.split(","));
    }
    CoreProperties.McpConfig mcp = new CoreProperties.McpConfig(endpoints);

    // Provider Overrides
    Map<String, CoreProperties.ProviderConfig> overrides = new HashMap<>();
    List.of("ollama", "openai")
        .forEach(
            provider -> {
              String prefix = "orasaka.core.overrides." + provider + ".";
              String model = env.getProperty(prefix + "model");
              String baseUrl = env.getProperty(prefix + "base-url");
              Double temp = env.getProperty(prefix + "temperature", Double.class);
              Integer maxTokens = env.getProperty(prefix + "max-tokens", Integer.class);

              Map<String, Object> extra = new HashMap<>();
              String embed = env.getProperty(prefix + "extra.embedding-model");
              if (embed != null) {
                extra.put("embedding-model", embed);
              }

              if (model != null || baseUrl != null) {
                overrides.put(
                    provider,
                    new CoreProperties.ProviderConfig(model, baseUrl, temp, maxTokens, extra));
              }
            });

    // Orchestration Config
    Boolean orchEnabled =
        env.getProperty("orasaka.core.orchestration.pipeline.enabled", Boolean.class, true);
    Boolean userContextEnabled =
        env.getProperty(
            "orasaka.core.orchestration.pipeline.user-context.enabled", Boolean.class, true);
    CoreProperties.UserContextConfig userContext =
        new CoreProperties.UserContextConfig(userContextEnabled);

    Boolean systemContextEnabled =
        env.getProperty(
            "orasaka.core.orchestration.pipeline.system-context.enabled", Boolean.class, true);
    CoreProperties.SystemContextConfig systemContext =
        new CoreProperties.SystemContextConfig(systemContextEnabled);

    Boolean refinerEnabled =
        env.getProperty("orasaka.core.orchestration.pipeline.refiner.enabled", Boolean.class, true);
    String refinerProvider =
        env.getProperty("orasaka.core.orchestration.pipeline.refiner.provider", "openai");
    String refinerModel =
        env.getProperty("orasaka.core.orchestration.pipeline.refiner.model", "gpt-4-turbo");
    Double refinerTemp =
        env.getProperty(
            "orasaka.core.orchestration.pipeline.refiner.temperature", Double.class, 0.2);
    CoreProperties.RefinerConfig refiner =
        new CoreProperties.RefinerConfig(
            refinerEnabled, refinerProvider, refinerModel, refinerTemp);

    Boolean routerEnabled =
        env.getProperty("orasaka.core.orchestration.pipeline.router.enabled", Boolean.class, true);
    String routerProvider =
        env.getProperty("orasaka.core.orchestration.pipeline.router.provider", "ollama");
    String routerModel =
        env.getProperty("orasaka.core.orchestration.pipeline.router.model", "llama3");
    Double routerTemp =
        env.getProperty(
            "orasaka.core.orchestration.pipeline.router.temperature", Double.class, 0.0);
    CoreProperties.RouterConfig router =
        new CoreProperties.RouterConfig(routerEnabled, routerProvider, routerModel, routerTemp);

    CoreProperties.OrchestrationConfig orchestration =
        new CoreProperties.OrchestrationConfig(
            orchEnabled, userContext, systemContext, refiner, router);

    return new CoreProperties(defaultProvider, overrides, rag, mcp, orchestration);
  }

  @Bean
  public OrasakaFeaturesProperties featuresProperties(Environment env) {
    Map<String, OrasakaFeaturesProperties.FeatureConfig> features = new HashMap<>();

    List.of("orasaka.core.chat.image", "orasaka.core.chat.speech", "orasaka.core.chat.text")
        .forEach(
            id -> {
              String prefix = "orasaka.features." + id + ".";
              Boolean enabled = env.getProperty(prefix + "enabled", Boolean.class, true);
              String label = env.getProperty(prefix + "label");
              String icon = env.getProperty(prefix + "icon");
              String uriPath = env.getProperty(prefix + "uriPath");
              String httpMethod = env.getProperty(prefix + "httpMethod");
              String payloadTemplate = env.getProperty(prefix + "payloadTemplate");

              if (id.equals("orasaka.core.chat.image")) {
                if (label == null) label = "Generate Image";
                if (icon == null) icon = "image";
                if (uriPath == null) uriPath = "/api/v1/chat/image";
                if (httpMethod == null) httpMethod = "POST";
                if (payloadTemplate == null) payloadTemplate = "{\"prompt\":\"${prompt}\"}";
              } else if (id.equals("orasaka.core.chat.speech")) {
                if (label == null) label = "Text to Speech";
                if (icon == null) icon = "mic";
                if (uriPath == null) uriPath = "/api/v1/chat/speech";
                if (httpMethod == null) httpMethod = "POST";
                if (payloadTemplate == null) payloadTemplate = "{\"text\":\"${text}\"}";
              } else { // orasaka.core.chat.text
                if (label == null) label = "Text Chat";
                if (icon == null) icon = "chat";
                if (uriPath == null) uriPath = "/api/v1/chat/stream";
                if (httpMethod == null) httpMethod = "POST";
                if (payloadTemplate == null) payloadTemplate = "{\"prompt\":\"${prompt}\"}";
              }

              features.put(
                  id,
                  new OrasakaFeaturesProperties.FeatureConfig(
                      enabled, label, icon, uriPath, httpMethod, payloadTemplate));
            });

    return new OrasakaFeaturesProperties(features);
  }

  @Bean
  public OrasakaAdminRegistry adminRegistry() {
    return new OrasakaAdminRegistry();
  }

  @Bean
  public OrasakaGraphEngine graphEngine(
      OrasakaFeaturesProperties featuresProperties, OrasakaAdminRegistry adminRegistry) {
    return new OrasakaGraphEngine(featuresProperties, adminRegistry);
  }
}
