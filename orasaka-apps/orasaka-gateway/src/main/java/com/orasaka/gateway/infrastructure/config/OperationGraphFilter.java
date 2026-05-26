package com.orasaka.gateway.infrastructure.config;

import com.orasaka.core.application.engine.GraphEngine;
import com.orasaka.core.domain.model.NodeState.Active;
import com.orasaka.core.domain.model.NodeState.Invisible;
import com.orasaka.core.domain.model.NodeState.Locked;
import com.orasaka.core.domain.model.OperationGraph;
import com.orasaka.core.domain.model.OperationNode;
import com.orasaka.core.domain.model.TargetExecutionUri;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/** Infrastructure filter verifying capability availability from the Orasaka Operation Graph. */
class OperationGraphFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperationGraphFilter.class);

  private final GraphEngine graphEngine;

  /**
   * Constructs the filter.
   *
   * @param graphEngine The graph engine evaluating capability states.
   */
  public OperationGraphFilter(GraphEngine graphEngine) {
    this.graphEngine = Objects.requireNonNull(graphEngine, "GraphEngine cannot be null");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    String method = request.getMethod();

    OperationGraph graph = graphEngine.compileGraph();

    if (path.equals("/graphql") && method.equalsIgnoreCase("POST")) {
      CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
      String body = wrappedRequest.getBody();

      for (OperationNode node : graph.nodes()) {
        String operationName = getGraphQLOperationName(node.id());
        if (body.contains(operationName)) {
          boolean allowed =
              switch (node.state()) {
                case Active active -> true;
                case Locked locked -> false;
                case Invisible invisible -> false;
              };

          if (!allowed) {
            LOGGER.warn(
                "Boundary rejection: GraphQL capability '{}' state is {}",
                operationName,
                node.state().getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response
                .getWriter()
                .write(
                    "{\"errors\": [{\"message\": \"Forbidden: Operation is currently unavailable\"}]}");
            return;
          }
        }
      }

      filterChain.doFilter(wrappedRequest, response);
      return;
    }

    Optional<OperationNode> matchingNode =
        graph.nodes().stream()
            .filter(node -> matches(path, method, node.executionDetails()))
            .findFirst();

    if (matchingNode.isPresent()) {
      OperationNode node = matchingNode.get();
      boolean allowed =
          switch (node.state()) {
            case Active active -> true;
            case Locked locked -> false;
            case Invisible invisible -> false;
          };

      if (!allowed) {
        LOGGER.warn(
            "Boundary rejection: REST capability '{}' state is {}",
            node.id(),
            node.state().getClass().getSimpleName());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response
            .getWriter()
            .write("{\"error\": \"Forbidden: Operation is currently unavailable\"}");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private boolean matches(String path, String method, TargetExecutionUri execution) {
    if (!method.equalsIgnoreCase(execution.httpMethod())) {
      return false;
    }
    String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    String cleanExecPath =
        execution.uriPath().endsWith("/")
            ? execution.uriPath().substring(0, execution.uriPath().length() - 1)
            : execution.uriPath();

    return cleanPath.startsWith(cleanExecPath) || cleanPath.equals(cleanExecPath);
  }

  private String getGraphQLOperationName(String id) {
    if (id.endsWith(".chat.text")) {
      return "chat";
    }
    if (id.endsWith(".chat.image")) {
      return "image";
    }
    if (id.endsWith(".chat.speech")) {
      return "speech";
    }
    return id;
  }
}
