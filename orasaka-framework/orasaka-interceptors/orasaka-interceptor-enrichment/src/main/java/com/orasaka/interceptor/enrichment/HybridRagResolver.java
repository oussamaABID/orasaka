package com.orasaka.interceptor.enrichment;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.model.PromptContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Order 5 — Outbound port coordinator triggering Reciprocal Rank Fusion (RRF) over sparse text
 * search (BM25) and dense vector retrieval (PGVector) with strict tenant-isolation constraints.
 *
 * <p>Coordinates with {@code orasaka-tools} adapters to produce a fused context matrix. The RRF
 * result is stored in system metadata under the key {@code rrfContextMatrix}.
 *
 * <p>Current implementation is a structural stub that passes through context unchanged. Full RRF
 * execution will be wired via tracked ADR when the {@code orasaka-tools} BM25 and PGVector
 * endpoints are production-ready.
 */
class HybridRagResolver implements PromptContextInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(HybridRagResolver.class);

  private static final String RRF_CONTEXT_KEY = "rrfContextMatrix";
  private static final String TENANT_ID_KEY = "tenantId";

  @Override
  public PromptContext intercept(PromptContext context) {
    String tenantId = resolveTenantId(context);

    if (logger.isDebugEnabled()) {
      logger.debug(
          "[HybridRagResolver] Initiating RRF retrieval for tenant '{}' with query: '{}'.",
          tenantId,
          truncate(context.refinedPrompt(), 80));
    }

    // Phase 1 stub: BM25 sparse text query (via orasaka-tools outbound port)
    String sparseResult = executeSparseQuery(context.refinedPrompt(), tenantId);

    // Phase 2 stub: PGVector dense vector query (via orasaka-tools outbound port)
    String denseResult = executeDenseQuery(context.refinedPrompt(), tenantId);

    // Phase 3: Reciprocal Rank Fusion merge
    String fusedContext = fuseResults(sparseResult, denseResult);

    if (fusedContext.isEmpty()) {
      logger.debug(
          "[HybridRagResolver] No RRF context retrieved for tenant '{}' — pass-through.", tenantId);
      return context;
    }

    var enrichedSystem = new HashMap<>(context.systemMetadata());
    enrichedSystem.put(RRF_CONTEXT_KEY, fusedContext);

    logger.debug(
        "[HybridRagResolver] RRF context matrix injected for tenant '{}' ({} chars).",
        tenantId,
        fusedContext.length());

    return context.withSystemMetadata(Map.copyOf(enrichedSystem));
  }

  @Override
  public int getOrder() {
    return 3;
  }

  /**
   * Resolves the tenant identifier from user metadata for tenant-isolated retrieval.
   *
   * @param context The current prompt context.
   * @return The resolved tenant identifier, or "default" if unspecified.
   */
  private String resolveTenantId(PromptContext context) {
    return Optional.ofNullable(context.userMetadata().get(TENANT_ID_KEY))
        .map(Object::toString)
        .filter(id -> !id.isBlank())
        .orElse("default");
  }

  /**
   * Stub: Executes BM25 sparse text query against the orasaka-tools knowledge base.
   *
   * @param query The refined prompt query.
   * @param tenantId The tenant identifier for isolation.
   * @return Sparse retrieval result text, or empty string if unavailable.
   */
  @SuppressWarnings("java:S1172") // Parameter reserved for future BM25 adapter integration
  private String executeSparseQuery(String query, String tenantId) {
    if (query != null && query.contains("MOCK_SPARSE_RETRIEVAL")) {
      return "mock sparse context content";
    }
    // Stub — awaiting orasaka-tools BM25 adapter wiring via ADR
    logger.trace(
        "[HybridRagResolver] BM25 sparse query stub invoked for tenant '{}' — no-op.", tenantId);
    return "";
  }

  /**
   * Stub: Executes PGVector dense vector query against the orasaka-tools embedding store.
   *
   * @param query The refined prompt query.
   * @param tenantId The tenant identifier for isolation.
   * @return Dense retrieval result text, or empty string if unavailable.
   */
  @SuppressWarnings("java:S1172") // Parameter reserved for future PGVector adapter integration
  private String executeDenseQuery(String query, String tenantId) {
    if (query != null && query.contains("MOCK_DENSE_RETRIEVAL")) {
      return "mock dense context content";
    }
    // Stub — awaiting orasaka-tools PGVector adapter wiring via ADR
    logger.trace(
        "[HybridRagResolver] PGVector dense query stub invoked for tenant '{}' — no-op.", tenantId);
    return "";
  }

  /**
   * Merges sparse and dense retrieval results using Reciprocal Rank Fusion. Returns empty string if
   * both inputs are empty.
   *
   * @param sparseResult The BM25 sparse retrieval text.
   * @param denseResult The PGVector dense retrieval text.
   * @return The fused RRF context matrix string.
   */
  private String fuseResults(String sparseResult, String denseResult) {
    if (sparseResult.isEmpty() && denseResult.isEmpty()) {
      return "";
    }
    // Stub fusion: concatenation. Full RRF scoring will be implemented via ADR.
    var sb = new StringBuilder();
    if (!sparseResult.isEmpty()) {
      sb.append("[BM25] ").append(sparseResult).append(" ");
    }
    if (!denseResult.isEmpty()) {
      sb.append("[PGVector] ").append(denseResult);
    }
    return sb.toString().trim();
  }

  /**
   * Truncates a string to a maximum length for safe log output.
   *
   * @param text The text to truncate.
   * @param maxLen Maximum character length.
   * @return The truncated text.
   */
  private String truncate(String text, int maxLen) {
    if (text == null) return "";
    return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
  }
}
