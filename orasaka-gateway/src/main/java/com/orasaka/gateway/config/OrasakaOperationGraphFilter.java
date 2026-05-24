package com.orasaka.gateway.config;

import com.orasaka.core.engine.*;
import com.orasaka.core.engine.NodeState.Active;
import com.orasaka.core.engine.NodeState.Invisible;
import com.orasaka.core.engine.NodeState.Locked;
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
class OrasakaOperationGraphFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaOperationGraphFilter.class);

  private final OrasakaGraphEngine graphEngine;

  /**
   * Constructs the filter.
   *
   * @param graphEngine The graph engine evaluating capability states.
   */
  public OrasakaOperationGraphFilter(OrasakaGraphEngine graphEngine) {
    this.graphEngine = Objects.requireNonNull(graphEngine, "OrasakaGraphEngine cannot be null");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    String method = request.getMethod();

    OrasakaOperationGraph graph = graphEngine.compileGraph();

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
            logger.warn(
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
        logger.warn(
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
