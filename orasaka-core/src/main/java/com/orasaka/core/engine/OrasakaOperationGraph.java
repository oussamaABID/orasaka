package com.orasaka.core.engine;

import java.util.List;

public final record OrasakaOperationGraph(List<OperationNode> nodes) {
  public OrasakaOperationGraph {
    nodes = (nodes == null) ? List.of() : List.copyOf(nodes);
  }
}
