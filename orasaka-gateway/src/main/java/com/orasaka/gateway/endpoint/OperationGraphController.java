package com.orasaka.gateway.endpoint;

import com.orasaka.core.engine.GraphEngine;
import com.orasaka.core.engine.OperationGraph;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing the compiled capability Operation Graph matrix. */
@RestController
public class OperationGraphController {

  private final GraphEngine graphEngine;

  /**
   * Constructs the controller.
   *
   * @param graphEngine The cognitive short-circuit graph compilation engine.
   */
  public OperationGraphController(GraphEngine graphEngine) {
    this.graphEngine = Objects.requireNonNull(graphEngine, "GraphEngine cannot be null");
  }

  /**
   * Compiles and outputs the operations graph.
   *
   * @return A JSON response containing the compiled {@link OperationGraph}.
   */
  @GetMapping("/api/v1/operations/graph")
  public ResponseEntity<OperationGraph> getGraph() {
    return ResponseEntity.ok(graphEngine.compileGraph());
  }
}
