package com.orasaka.tools.mcp;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.mcp.McpOrchestrator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Default implementation for MCP Orchestrator. Bridges external MCP server capabilities into the
 * Orasaka engine.
 */
@Component
public class DefaultMcpOrchestrator implements McpOrchestrator {

  private final CoreProperties properties;

  /**
   * Instantiates a new DefaultMcpOrchestrator using the provided configuration properties.
   *
   * @param properties Configuration properties mapping the target MCP server endpoints.
   * @see com.orasaka.core.config.CoreProperties
   */
  public DefaultMcpOrchestrator(CoreProperties properties) {
    this.properties = properties;
  }

  /**
   * Resolves context from external MCP endpoints using parallel HTTP requests.
   *
   * <p>Utilizes Java 21 HttpClient and CompletableFuture for efficient, non-blocking retrieval from
   * multiple servers on Virtual Threads.
   *
   * @return A consolidated string containing external context from all reachable servers.
   */
  @Override
  public String resolveExternalContext() {
    if (properties.mcp() == null
        || properties.mcp().endpoints() == null
        || properties.mcp().endpoints().isEmpty()) {
      return "";
    }

    try (HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .executor(Executors.newVirtualThreadPerTaskExecutor()) // Enforce Virtual Threads
            .build()) {

      List<CompletableFuture<String>> futures =
          properties.mcp().endpoints().stream()
              .map(
                  endpoint -> {
                    HttpRequest request =
                        HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();

                    return client
                        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .exceptionally(ex -> ""); // Silently fail for individual endpoints
                  })
              .toList();

      CompletableFuture<?>[] array = futures.toArray(new CompletableFuture<?>[0]);
      return CompletableFuture.allOf(array)
          .thenApply(
              v ->
                  futures.stream()
                      .map(CompletableFuture::join)
                      .filter(s -> !s.isBlank())
                      .collect(Collectors.joining("\n")))
          .get();
    } catch (Exception e) {
      return ""; // Fallback to empty context on global failure
    }
  }

  /**
   * Resolves tools from external Model Context Protocol (MCP) server endpoints.
   *
   * <p>Under local dev environments, this returns custom tool registrations matching the target
   * client criteria.
   *
   * @return A list of external tool definition schema configurations.
   * @see com.orasaka.core.mcp.McpOrchestrator
   */
  @Override
  public List<Object> resolveExternalTools() {
    // Future: Fetch tool definitions from /tools endpoint of MCP servers
    return new ArrayList<>();
  }
}
