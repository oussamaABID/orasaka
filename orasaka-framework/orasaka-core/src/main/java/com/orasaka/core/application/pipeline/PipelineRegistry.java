package com.orasaka.core.application.pipeline;

import com.orasaka.core.domain.model.ConditionalRoute;
import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.model.PipelineConfig;
import com.orasaka.core.domain.ports.outbound.PipelineConfigProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thread-safe, hot-reloadable registry for pipeline configurations.
 *
 * <p>Maintains an {@link AtomicReference} over an immutable {@code Map<String, PipelineConfig>}
 * snapshot. When an administrator updates pipeline configurations in the database, calling {@link
 * #reload()} atomically replaces the in-memory map without interrupting active SSE streams — those
 * streams hold references to the previous immutable snapshot and continue undisturbed.
 *
 * <p><b>Concurrency Contract</b>:
 *
 * <ul>
 *   <li>Readers ({@link #getConfig}, {@link #getActiveInterceptorIds}) perform a single {@code
 *       AtomicReference.get()} — lock-free, wait-free.
 *   <li>Writers ({@link #reload}) build a new immutable map and swap it atomically via {@code
 *       AtomicReference.set()}.
 *   <li>No {@code synchronized} blocks, no locks over I/O — fully Virtual Thread safe.
 * </ul>
 */
@Component
public final class PipelineRegistry {

  private static final Logger logger = LoggerFactory.getLogger(PipelineRegistry.class);

  /**
   * Phase 1 core interceptor keys — mandatory, non-bypassable. These interceptors execute for ALL
   * requests regardless of routing mode or admin configuration.
   */
  private static final List<String> CORE_INTERCEPTOR_KEYS =
      List.of("UserContextResolver", "SystemContextInjector", "RagInterceptor");

  /** Default conditional routes for semantic routing. */
  private static final List<ConditionalRoute> DEFAULT_ROUTES =
      List.of(
          new ConditionalRoute(
              "video_generation", List.of("MediaInterceptor", "ToolInterceptor"), 0.70),
          new ConditionalRoute("translation_required", List.of("RefinerInterceptor"), 0.60),
          new ConditionalRoute(
              "strict_json_format", List.of("RefinerInterceptor", "RouterInterceptor"), 0.65));

  private final PipelineConfigProvider configProvider;
  private final AtomicReference<Map<String, PipelineConfig>> configCache;

  /**
   * Constructs the registry and performs the initial load from the database.
   *
   * @param configProvider Database-driven pipeline config provider.
   */
  public PipelineRegistry(PipelineConfigProvider configProvider) {
    this.configProvider = configProvider;
    this.configCache = new AtomicReference<>(Map.of());
    reload();
  }

  /**
   * Returns the pipeline configuration for the given ID.
   *
   * @param pipelineId The pipeline identifier.
   * @return The pipeline config, or a default config if not found.
   */
  public PipelineConfig getConfig(String pipelineId) {
    String key = (pipelineId != null) ? pipelineId : PipelineConfig.DEFAULT_PIPELINE_ID;
    Map<String, PipelineConfig> snapshot = configCache.get();
    return snapshot.getOrDefault(key, buildDefaultConfig());
  }

  /**
   * Returns all active interceptor IDs for a given pipeline (union of core + dynamic).
   *
   * @param pipelineId The pipeline identifier.
   * @return Combined list of interceptor keys.
   */
  public List<String> getActiveInterceptorIds(String pipelineId) {
    PipelineConfig config = getConfig(pipelineId);
    List<String> combined = new ArrayList<>(config.coreInterceptorKeys());
    combined.addAll(config.dynamicInterceptorKeys());
    return List.copyOf(combined);
  }

  /**
   * Atomically reloads the pipeline configuration from the database.
   *
   * <p>Active SSE streams referencing the old snapshot are unaffected — they hold immutable
   * references that remain valid until garbage collected.
   */
  public void reload() {
    try {
      List<InterceptorConfig> allConfigs = configProvider.findAllOrdered();
      PipelineConfig pipelineConfig = buildFromDbConfigs(allConfigs);
      configCache.set(Map.of(pipelineConfig.pipelineId(), pipelineConfig));
      logger.info(
          "PipelineRegistry reloaded — {} core, {} dynamic interceptor(s), {} route(s).",
          pipelineConfig.coreInterceptors().size(),
          pipelineConfig.dynamicInterceptors().size(),
          pipelineConfig.routes().size());
    } catch (Exception e) {
      logger.warn("PipelineRegistry reload failed — retaining previous snapshot.", e);
      if (configCache.get().isEmpty()) {
        PipelineConfig fallback = buildDefaultConfig();
        configCache.set(Map.of(fallback.pipelineId(), fallback));
        logger.info("PipelineRegistry initialized with default configuration.");
      }
    }
  }

  /**
   * Builds a pipeline config from database interceptor configurations.
   *
   * <p>Splits the flat DB list into core (Phase 1) and dynamic (Phase 2) based on the hardcoded
   * core interceptor key set.
   */
  private PipelineConfig buildFromDbConfigs(List<InterceptorConfig> allConfigs) {
    List<InterceptorConfig> core = new ArrayList<>();
    List<InterceptorConfig> dynamic = new ArrayList<>();

    for (InterceptorConfig config : allConfigs) {
      if (CORE_INTERCEPTOR_KEYS.contains(config.interceptorKey())) {
        core.add(config);
      } else {
        dynamic.add(config);
      }
    }

    // Ensure all core interceptors are present even if not in DB
    for (String coreKey : CORE_INTERCEPTOR_KEYS) {
      boolean exists = core.stream().anyMatch(c -> c.interceptorKey().equals(coreKey));
      if (!exists) {
        core.add(
            new InterceptorConfig(
                coreKey, humanize(coreKey), CORE_INTERCEPTOR_KEYS.indexOf(coreKey), true, ""));
      }
    }

    return new PipelineConfig(PipelineConfig.DEFAULT_PIPELINE_ID, core, dynamic, DEFAULT_ROUTES);
  }

  /**
   * Builds a default pipeline config with hardcoded core interceptors and empty dynamic set.
   *
   * @return A minimal default {@link PipelineConfig}.
   */
  private static PipelineConfig buildDefaultConfig() {
    List<InterceptorConfig> core = new ArrayList<>();
    for (int i = 0; i < CORE_INTERCEPTOR_KEYS.size(); i++) {
      String key = CORE_INTERCEPTOR_KEYS.get(i);
      core.add(new InterceptorConfig(key, humanize(key), i, true, ""));
    }
    return new PipelineConfig(PipelineConfig.DEFAULT_PIPELINE_ID, core, List.of(), DEFAULT_ROUTES);
  }

  /**
   * Converts a PascalCase class name into a human-readable label.
   *
   * @param className The simple class name.
   * @return A space-separated human-readable string.
   */
  private static String humanize(String className) {
    return PipelineUtils.humanize(className);
  }
}
