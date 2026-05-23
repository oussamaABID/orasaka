package com.orasaka.core.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Short-circuit compilation engine resolving static configurations and runtime lock registries. */
public class OrasakaGraphEngine {

  private final OrasakaFeaturesProperties properties;
  private final OrasakaAdminRegistry adminRegistry;

  /**
   * Constructs the engine.
   *
   * @param properties Static capability blueprints.
   * @param adminRegistry Dynamic runtime lock store.
   */
  public OrasakaGraphEngine(
      OrasakaFeaturesProperties properties, OrasakaAdminRegistry adminRegistry) {
    this.properties = Objects.requireNonNull(properties, "Features properties cannot be null");
    this.adminRegistry =
        Objects.requireNonNull(adminRegistry, "Admin lock registry cannot be null");
  }

  /**
   * Compiles static capability parameters with dynamic locks.
   *
   * <p>Short-circuits evaluation: If configuration disables a feature, dynamic data checks are
   * bypassed entirely and node evaluates to {@link Invisible}.
   *
   * @return A compiled {@link OrasakaOperationGraph} matrix instance.
   */
  public OrasakaOperationGraph compileGraph() {
    List<OperationNode> nodes = new ArrayList<>();

    properties
        .features()
        .forEach(
            (id, config) -> {
              // Short-circuit evaluations instantly
              if (!config.enabled()) {
                nodes.add(
                    new OperationNode(
                        id,
                        config.label(),
                        config.icon(),
                        "CONTEXT_MENU_PLUS",
                        new Invisible(),
                        new TargetExecutionUri(
                            config.uriPath(), config.httpMethod(), config.payloadTemplate())));
                return;
              }

              // Check admin lock status
              NodeState state =
                  adminRegistry
                      .getLock(id)
                      .<NodeState>map(lock -> new Locked(lock.reason(), lock.lockedAt()))
                      .orElse(new Active());

              nodes.add(
                  new OperationNode(
                      id,
                      config.label(),
                      config.icon(),
                      "CONTEXT_MENU_PLUS",
                      state,
                      new TargetExecutionUri(
                          config.uriPath(), config.httpMethod(), config.payloadTemplate())));
            });

    return new OrasakaOperationGraph(nodes);
  }
}
