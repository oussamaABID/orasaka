package com.orasaka.core.engine;

import com.orasaka.core.engine.NodeState.Active;
import com.orasaka.core.engine.NodeState.Invisible;
import com.orasaka.core.engine.NodeState.Locked;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Short-circuit compilation engine resolving static configurations and runtime lock registries. */
public class GraphEngine {

  private final FeaturesProperties properties;
  private final AdminRegistry adminRegistry;

  /**
   * Constructs the engine.
   *
   * @param properties Static capability blueprints.
   * @param adminRegistry Dynamic runtime lock store.
   */
  public GraphEngine(FeaturesProperties properties, AdminRegistry adminRegistry) {
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
   * @return A compiled {@link OperationGraph} matrix instance.
   */
  public OperationGraph compileGraph() {
    List<OperationNode> nodes = new ArrayList<>();

    properties
        .features()
        .forEach(
            (id, config) -> {
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

    return new OperationGraph(nodes);
  }
}
