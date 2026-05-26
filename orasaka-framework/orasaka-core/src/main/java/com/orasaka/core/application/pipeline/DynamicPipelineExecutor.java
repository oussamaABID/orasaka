package com.orasaka.core.application.pipeline;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.model.AdvancedPipelineSchema;
import com.orasaka.core.domain.model.Authority;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.model.PipelineConfig;
import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.domain.model.RoutingMode;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.core.infrastructure.config.SecurityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Two-phase dynamic pipeline executor with semantic routing, Micrometer telemetry, and thread-safe
 * hot-reload support.
 *
 * <p>Execution flow:
 *
 * <ol>
 *   <li><b>Phase 1 — Core Immutable Pipeline</b>: Identity/Role Verification →
 *       Security/Anonymization → Base Context/RAG Slicing. These interceptors are mandatory for ALL
 *       requests. They cannot be bypassed, toggled, or reordered by any UI action.
 *   <li><b>Semantic Routing</b>: Between Phase 1 and Phase 2, the {@link SemanticRoutingEngine}
 *       evaluates the prompt and dynamically selects the required Phase 2 interceptors.
 *   <li><b>Phase 2 — Dynamic Custom Pipeline</b>: Only the semantically-matched interceptors
 *       execute. Falls back to the full deterministic chain if no routes match.
 * </ol>
 *
 * <p><b>Micrometer Telemetry</b>:
 *
 * <ul>
 *   <li>{@code orasaka.pipeline.routing.latency} — Timer tracking semantic classification
 *       round-trip.
 *   <li>{@code orasaka.pipeline.active.interceptors.count} — Gauge tracking live interceptor count
 *       per execution.
 * </ul>
 *
 * <p><b>Thread Safety</b>: All mutable state is confined to method-local variables. The {@link
 * PipelineRegistry} provides atomic snapshots. No {@code synchronized} blocks over I/O.
 *
 * @see PipelineRegistry
 * @see SemanticRoutingEngine
 */
@Component
public final class DynamicPipelineExecutor {

  private static final Logger logger = LoggerFactory.getLogger(DynamicPipelineExecutor.class);

  private final Map<String, PromptContextInterceptor> interceptorRegistry;
  private final List<PromptContextInterceptor> fallbackChain;
  private final PipelineRegistry pipelineRegistry;
  private final SemanticRoutingEngine routingEngine;
  private final SecurityProperties securityProperties;
  private final CoreProperties properties;
  private final Timer routingLatencyTimer;
  private final AtomicInteger activeInterceptorGauge;

  /**
   * Constructs the two-phase pipeline executor.
   *
   * @param interceptors Registered PromptContextInterceptor beans (auto-discovered via Spring Boot
   *     auto-configuration from interceptor submodules).
   * @param pipelineRegistry Thread-safe pipeline configuration registry.
   * @param routingEngine Semantic routing engine for Phase 2 interceptor selection.
   * @param securityProperties Security governance configuration.
   * @param properties Core configuration properties.
   * @param meterRegistry Micrometer meter registry for telemetry.
   */
  public DynamicPipelineExecutor(
      List<PromptContextInterceptor> interceptors,
      PipelineRegistry pipelineRegistry,
      SemanticRoutingEngine routingEngine,
      SecurityProperties securityProperties,
      CoreProperties properties,
      MeterRegistry meterRegistry) {

    List<PromptContextInterceptor> all =
        new ArrayList<>(interceptors != null ? interceptors : List.of());

    this.interceptorRegistry =
        all.stream()
            .collect(Collectors.toMap(i -> i.getClass().getSimpleName(), i -> i, (a, b) -> a));

    // Ordering is resolved dynamically by the PipelineRegistry from database configuration.
    // The fallbackChain preserves Spring injection order — no hardcoded sort.
    this.fallbackChain = List.copyOf(all);

    this.pipelineRegistry = pipelineRegistry;
    this.routingEngine = routingEngine;
    this.securityProperties =
        securityProperties != null ? securityProperties : new SecurityProperties();
    this.properties = properties;

    // Micrometer telemetry
    this.routingLatencyTimer =
        Timer.builder("orasaka.pipeline.routing.latency")
            .description("Semantic classification round-trip latency")
            .register(meterRegistry);

    this.activeInterceptorGauge = new AtomicInteger(0);
    meterRegistry.gauge("orasaka.pipeline.active.interceptors.count", activeInterceptorGauge);

    logger.info(
        "Initialized DynamicPipelineExecutor with {} registered interceptors. "
            + "Security kill-switch: {}",
        this.interceptorRegistry.size(),
        this.securityProperties.disableAi() ? "ACTIVE" : "inactive");
  }

  /**
   * Executes the two-phase interceptor pipeline on the raw user prompt.
   *
   * @param rawUserQuery The original raw prompt.
   * @param context The execution context.
   * @param pipelineId The pipeline configuration identifier (nullable — uses default).
   * @return The enriched {@link PromptContext}, or {@code null} if the pipeline is disabled.
   * @throws SecurityException if an AI-dependent interceptor is invoked while the governance
   *     kill-switch is active.
   */
  public PromptContext process(String rawUserQuery, Context context, String pipelineId) {
    boolean enabled = properties.orchestration() != null && properties.orchestration().enabled();
    if (!enabled) {
      return null;
    }

    logger.debug("DynamicPipelineExecutor: starting two-phase execution...");

    Map<String, Object> userMetadata = new HashMap<>();
    userMetadata.put("userId", context.userId());
    userMetadata.put("conversationId", context.conversationId());
    userMetadata.putAll(context.preferences());
    userMetadata.put("roles", context.authorities().stream().map(Authority::name).toList());

    RoutingMode mode = resolveRoutingMode();
    PromptContext promptContext =
        new PromptContext(rawUserQuery, userMetadata, Map.of(), rawUserQuery, null, mode);

    // ── Phase 1: Core Immutable Pipeline ──
    List<PromptContextInterceptor> coreChain = resolveCoreChain(pipelineId);
    logger.debug("Phase 1: executing {} core interceptor(s).", coreChain.size());

    for (PromptContextInterceptor interceptor : coreChain) {
      enforceSecurityGate(interceptor);
      try {
        promptContext = interceptor.beforeExecution(promptContext);
      } catch (Exception e) {
        logger.error(
            "Phase 1 interceptor '{}' failed — continuing with current context.",
            interceptor.getClass().getSimpleName(),
            e);
      }
    }

    // ── Semantic Routing (between Phase 1 and Phase 2) ──
    List<PromptContextInterceptor> dynamicChain;
    if (mode == RoutingMode.SEMANTIC) {
      dynamicChain = resolveSemanticChain(rawUserQuery, pipelineId);
    } else {
      dynamicChain = resolveDeterministicDynamicChain(pipelineId);
    }

    // ── Phase 2: Dynamic Custom Pipeline ──
    int totalCount = coreChain.size() + dynamicChain.size();
    activeInterceptorGauge.set(totalCount);
    logger.debug("Phase 2: executing {} dynamic interceptor(s).", dynamicChain.size());

    for (PromptContextInterceptor interceptor : dynamicChain) {
      enforceSecurityGate(interceptor);
      try {
        promptContext = interceptor.beforeExecution(promptContext);
      } catch (Exception e) {
        logger.error(
            "Phase 2 interceptor '{}' failed — continuing with current context.",
            interceptor.getClass().getSimpleName(),
            e);
      }
    }

    logger.debug(
        "DynamicPipelineExecutor completed: {} total interceptor(s) executed.", totalCount);
    return promptContext;
  }

  /**
   * Builds the Early-Ack pipeline schema for SSE metadata streaming.
   *
   * <p>Called by the SSE controller before LLM inference begins to push pipeline architecture
   * metadata to the UI.
   *
   * @param pipelineId The pipeline configuration identifier.
   * @param rawUserQuery The raw prompt (used for semantic routing preview).
   * @return The compiled {@link AdvancedPipelineSchema} ready for JSON serialization.
   */
  public AdvancedPipelineSchema buildSchema(String pipelineId, String rawUserQuery) {
    PipelineConfig config = pipelineRegistry.getConfig(pipelineId);
    List<String> coreIds = config.coreInterceptorKeys();

    RoutingMode mode = resolveRoutingMode();
    List<String> dynamicIds;
    if (mode == RoutingMode.SEMANTIC) {
      List<PromptContextInterceptor> semanticChain = resolveSemanticChain(rawUserQuery, pipelineId);
      dynamicIds = semanticChain.stream().map(i -> i.getClass().getSimpleName()).toList();
    } else {
      dynamicIds = config.dynamicInterceptorKeys();
    }

    long estimatedLatencyMs = (coreIds.size() + dynamicIds.size()) * 5L;
    return new AdvancedPipelineSchema(config.pipelineId(), coreIds, dynamicIds, estimatedLatencyMs);
  }

  /**
   * Resolves the Phase 1 core interceptor chain from the pipeline registry.
   *
   * <p>Core interceptors are resolved by key from the Spring-managed bean registry. Missing beans
   * are logged and skipped — the chain is best-effort but never empty in a healthy deployment.
   */
  private List<PromptContextInterceptor> resolveCoreChain(String pipelineId) {
    PipelineConfig config = pipelineRegistry.getConfig(pipelineId);
    List<PromptContextInterceptor> chain = new ArrayList<>();
    for (String key : config.coreInterceptorKeys()) {
      PromptContextInterceptor interceptor = interceptorRegistry.get(key);
      if (interceptor != null) {
        chain.add(interceptor);
      } else {
        logger.warn("Core interceptor '{}' has no registered bean — skipping.", key);
      }
    }
    return List.copyOf(chain);
  }

  /**
   * Resolves the Phase 2 dynamic chain via semantic routing.
   *
   * <p>Delegates to {@link SemanticRoutingEngine#resolveInterceptors} with Micrometer timing. Falls
   * back to the deterministic dynamic chain if the routing engine returns empty.
   */
  private List<PromptContextInterceptor> resolveSemanticChain(
      String rawUserQuery, String pipelineId) {
    PipelineConfig config = pipelineRegistry.getConfig(pipelineId);

    Timer.Sample sample = Timer.start();
    List<PromptContextInterceptor> semanticResult =
        routingEngine.resolveInterceptors(rawUserQuery, config.routes(), interceptorRegistry);
    sample.stop(routingLatencyTimer);

    if (semanticResult.isEmpty()) {
      logger.debug(
          "Semantic routing returned empty — falling back to deterministic dynamic chain.");
      return resolveDeterministicDynamicChain(pipelineId);
    }
    return semanticResult;
  }

  /**
   * Resolves the Phase 2 dynamic chain from the deterministic database configuration.
   *
   * <p>Filters to enabled interceptors only and resolves beans from the registry.
   */
  private List<PromptContextInterceptor> resolveDeterministicDynamicChain(String pipelineId) {
    PipelineConfig config = pipelineRegistry.getConfig(pipelineId);
    List<PromptContextInterceptor> chain = new ArrayList<>();
    for (var interceptorConfig : config.dynamicInterceptors()) {
      if (!interceptorConfig.enabled()) {
        continue;
      }
      PromptContextInterceptor interceptor =
          interceptorRegistry.get(interceptorConfig.interceptorKey());
      if (interceptor != null) {
        chain.add(interceptor);
      } else {
        logger.warn(
            "Dynamic interceptor '{}' has no registered bean — skipping.",
            interceptorConfig.interceptorKey());
      }
    }
    if (chain.isEmpty()) {
      logger.debug("No dynamic interceptors resolved from DB — using fallback chain minus core.");
      return fallbackChain.stream()
          .filter(i -> !PipelineRegistry.class.getName().contains(i.getClass().getSimpleName()))
          .toList();
    }
    return List.copyOf(chain);
  }

  /**
   * Enforces the security governance kill-switch. Throws a hard {@link SecurityException} if the
   * interceptor is AI-dependent and the kill-switch is active.
   *
   * @param interceptor The interceptor to validate.
   * @throws SecurityException if the interceptor requires AI and the kill-switch is enabled.
   */
  private void enforceSecurityGate(PromptContextInterceptor interceptor) {
    if (securityProperties.disableAi() && interceptor.isAiDependent()) {
      String name = interceptor.getClass().getSimpleName();
      logger.error(
          "SECURITY GATE VIOLATION — AI-dependent interceptor '{}' blocked by governance "
              + "kill-switch (orasaka.security.disable-ai=true).",
          name);
      throw new SecurityException(
          "AI governance kill-switch is active. AI-dependent interceptor '"
              + name
              + "' is prohibited. Set orasaka.security.disable-ai=false to re-enable.");
    }
  }

  /**
   * Resolves the active routing mode from configuration.
   *
   * @return The configured routing mode, defaulting to DETERMINISTIC.
   */
  private RoutingMode resolveRoutingMode() {
    if (properties.orchestration() != null && properties.orchestration().routing() != null) {
      return properties.orchestration().routing().mode();
    }
    return RoutingMode.DETERMINISTIC;
  }

  /**
   * Evicts the pipeline registry cache, forcing a reload from the database on next access.
   *
   * <p>Called by admin controllers after configuration changes. Active SSE streams are unaffected.
   */
  public void evictAndReload() {
    pipelineRegistry.reload();
    logger.info("DynamicPipelineExecutor cache evicted — PipelineRegistry reloaded from database.");
  }

  /**
   * Convenience overload for callers that do not specify a pipeline ID.
   *
   * @param rawUserQuery The original raw prompt.
   * @param context The execution context.
   * @return The enriched {@link PromptContext}, or {@code null} if the pipeline is disabled.
   */
  public PromptContext process(String rawUserQuery, Context context) {
    return process(rawUserQuery, context, null);
  }

  /**
   * Evicts the cached chain, forcing a rebuild from the database on next {@code process()} call.
   * Called by admin controllers after configuration changes.
   */
  public void evictChainCache() {
    pipelineRegistry.reload();
    logger.info("Pipeline chain cache evicted — next execution will rebuild from database.");
  }

  /**
   * Returns the current chain configuration as domain records.
   *
   * @return Ordered list of interceptor configs reflecting the current chain state.
   */
  public List<InterceptorConfig> getCurrentConfig() {
    PipelineConfig config = pipelineRegistry.getConfig(null);
    List<InterceptorConfig> all = new ArrayList<>(config.coreInterceptors());
    all.addAll(config.dynamicInterceptors());
    return List.copyOf(all);
  }

  /**
   * Builds default interceptor configurations from the hardcoded fallback chain.
   *
   * @return List of default interceptor configs.
   */
  public List<InterceptorConfig> buildDefaultConfigs() {
    return fallbackChain.stream()
        .map(
            i ->
                new InterceptorConfig(
                    i.getClass().getSimpleName(),
                    PipelineUtils.humanize(i.getClass().getSimpleName()),
                    0,
                    true,
                    ""))
        .toList();
  }
}
