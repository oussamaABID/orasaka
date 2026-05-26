package com.orasaka.core.application.engine;

import com.orasaka.core.domain.model.NodeState;
import com.orasaka.core.domain.model.NodeState.Active;
import com.orasaka.core.domain.model.NodeState.Invisible;
import com.orasaka.core.domain.model.NodeState.Locked;
import com.orasaka.core.domain.model.OperationGraph;
import com.orasaka.core.domain.model.OperationNode;
import com.orasaka.core.domain.model.TargetExecutionUri;
import com.orasaka.core.domain.ports.outbound.AdminRegistry;
import com.orasaka.core.domain.ports.outbound.FeatureToggleProvider;
import com.orasaka.core.domain.ports.outbound.ModelCatalogProvider;
import com.orasaka.core.infrastructure.config.FeaturesProperties;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Short-circuit compilation engine resolving static configurations and runtime lock registries. */
public class GraphEngine {

  private final FeaturesProperties properties;
  private final AdminRegistry adminRegistry;
  private final FeatureToggleProvider featureToggleProvider;
  private final InfrastructureProber prober;
  private final ModelCatalogProvider modelCatalogProvider;

  /**
   * Constructs the engine.
   *
   * @param properties Static capability blueprints.
   * @param adminRegistry Dynamic runtime lock store.
   * @param featureToggleProvider Provider for database overrides on feature toggles.
   * @param prober Asynchronous infrastructure status prober.
   * @param modelCatalogProvider The model catalog provider to resolve active models dynamically.
   */
  public GraphEngine(
      FeaturesProperties properties,
      AdminRegistry adminRegistry,
      FeatureToggleProvider featureToggleProvider,
      InfrastructureProber prober,
      ModelCatalogProvider modelCatalogProvider) {
    this.properties = Objects.requireNonNull(properties, "Features properties cannot be null");
    this.adminRegistry =
        Objects.requireNonNull(adminRegistry, "Admin lock registry cannot be null");
    this.featureToggleProvider = featureToggleProvider;
    this.prober = Objects.requireNonNull(prober, "Infrastructure prober cannot be null");
    this.modelCatalogProvider = modelCatalogProvider;
  }

  /** Cache TTL in seconds. The graph changes rarely (feature toggles, model registration). */
  private static final long CACHE_TTL_SECONDS = 30;

  /** Immutable cache entry holding both graph and timestamp atomically. */
  private record CacheEntry(OperationGraph graph, Instant timestamp) {}

  /** Thread-safe cached graph result to avoid redundant computation on frequent UI polls. */
  private final AtomicReference<CacheEntry> cache =
      new AtomicReference<>(new CacheEntry(null, Instant.EPOCH));

  /**
   * Constructs the engine without a feature toggle provider (fallback).
   *
   * @param properties Static capability blueprints.
   * @param adminRegistry Dynamic runtime lock store.
   */
  public GraphEngine(FeaturesProperties properties, AdminRegistry adminRegistry) {
    this(properties, adminRegistry, null, new InfrastructureProber(null, 8188, 8085), null);
  }

  /**
   * Compiles static capability parameters with dynamic locks and database overrides.
   *
   * <p>Short-circuits evaluation: If configuration or database disables a feature, dynamic data
   * checks are bypassed entirely and node evaluates to {@link Invisible}.
   *
   * @return A compiled {@link OperationGraph} matrix instance.
   */
  public OperationGraph compileGraph() {
    CacheEntry entry = cache.get();
    if (entry.graph() != null
        && Instant.now().isBefore(entry.timestamp().plusSeconds(CACHE_TTL_SECONDS))) {
      return entry.graph();
    }
    OperationGraph result = doCompileGraph();
    cache.set(new CacheEntry(result, Instant.now()));
    return result;
  }

  /**
   * Invalidates the cached graph, forcing recompilation on the next call. Should be invoked when
   * features are toggled or models change.
   */
  public void invalidateCache() {
    cache.set(new CacheEntry(null, Instant.EPOCH));
  }

  /** Internal compilation logic, extracted for caching wrapper. */
  private OperationGraph doCompileGraph() {
    List<OperationNode> nodes = new ArrayList<>();
    final String activeModel =
        (modelCatalogProvider != null)
            ? modelCatalogProvider.getActiveChatModel().orElse(null)
            : null;

    properties
        .features()
        .forEach(
            (id, config) -> {
              boolean enabled = resolveEnabled(id, config);
              NodeState state = enabled ? resolveNodeState(id, activeModel) : new Invisible();
              nodes.add(buildNode(id, config, state));
            });

    return new OperationGraph(nodes);
  }

  /** Resolves whether a feature is enabled, considering database overrides. */
  private boolean resolveEnabled(String id, FeaturesProperties.FeatureConfig config) {
    boolean enabled = config.enabled();
    if (featureToggleProvider != null) {
      enabled = featureToggleProvider.isEnabled(id).orElse(enabled);
    }
    return enabled;
  }

  /** Resolves the runtime node state for an enabled feature. */
  private NodeState resolveNodeState(String id, String activeModel) {
    if (activeModel == null) {
      return new Locked(
          "Capability missing: No active Ollama chat model found in registry", LocalDateTime.now());
    }
    if ("orasaka.core.media.video".equals(id)) {
      return resolveMediaState(id);
    }
    return resolveAdminLockState(id);
  }

  /** Resolves state for the video/image media feature, checking infrastructure probes. */
  private NodeState resolveMediaState(String id) {
    boolean videoOnline = prober != null && prober.isVideoEngineOnline();
    boolean imageOnline = prober != null && prober.isImageEngineOnline();
    if (!videoOnline && !imageOnline) {
      int vPort = prober != null ? prober.getVideoProbePort() : 8188;
      int iPort = prober != null ? prober.getImageProbePort() : 8085;
      return new Locked(
          "Video engine offline on port "
              + vPort
              + " and fallback image engine offline on port "
              + iPort,
          LocalDateTime.now());
    }
    return resolveAdminLockState(id);
  }

  /** Resolves state from admin lock registry, defaulting to Active. */
  private NodeState resolveAdminLockState(String id) {
    return adminRegistry
        .getLock(id)
        .<NodeState>map(lock -> new Locked(lock.reason(), lock.lockedAt()))
        .orElse(new Active());
  }

  /** Builds an OperationNode from feature config and resolved state. */
  private static OperationNode buildNode(
      String id, FeaturesProperties.FeatureConfig config, NodeState state) {
    return new OperationNode(
        id,
        config.label(),
        config.icon(),
        "CONTEXT_MENU_PLUS",
        state,
        new TargetExecutionUri(config.uriPath(), config.httpMethod(), config.payloadTemplate()));
  }
}
