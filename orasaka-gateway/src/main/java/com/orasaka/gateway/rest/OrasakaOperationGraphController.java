package com.orasaka.gateway.rest;

import com.orasaka.core.graph.OrasakaGraphEngine;
import com.orasaka.core.graph.OrasakaOperationGraph;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing the compiled capability Operation Graph matrix. */
@RestController
public class OrasakaOperationGraphController {

  private final OrasakaGraphEngine graphEngine;

  /**
   * Constructs the controller.
   *
   * @param graphEngine The cognitive short-circuit graph compilation engine.
   */
  public OrasakaOperationGraphController(OrasakaGraphEngine graphEngine) {
    this.graphEngine = Objects.requireNonNull(graphEngine, "OrasakaGraphEngine cannot be null");
  }

  /**
   * Compiles and outputs the operations graph.
   *
   * @return A JSON response containing the compiled {@link OrasakaOperationGraph}.
   */
  @GetMapping("/api/v1/operations/graph")
  public ResponseEntity<OrasakaOperationGraph> getGraph() {
    return ResponseEntity.ok(graphEngine.compileGraph());
  }
}
