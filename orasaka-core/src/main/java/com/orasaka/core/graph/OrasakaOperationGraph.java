package com.orasaka.core.graph;

import java.util.List;

/**
 * Root matrix model containing all operations nodes in the capability tree.
 *
 * @param nodes The operations capability list.
 */
public final record OrasakaOperationGraph(List<OperationNode> nodes) {
  public OrasakaOperationGraph {
    nodes = (nodes == null) ? List.of() : List.copyOf(nodes);
  }
}
