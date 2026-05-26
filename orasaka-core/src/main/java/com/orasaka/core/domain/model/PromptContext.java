package com.orasaka.core.domain.model;

import java.util.HashMap;
import java.util.Map;

/**
 * State machine context holding user and system matrices for prompt refinement and routing.
 *
 * <p>Implemented as an immutable Java 21 record in compliance with ADR-008.
 *
 * @param rawUserQuery The original raw user query prompt.
 * @param userMetadata Immutable map containing enriched user attributes and preferences.
 * @param systemMetadata Immutable map containing enriched system and environment signals.
 * @param refinedPrompt The refined instruction text (defaults to rawUserQuery).
 * @param routedProvider The routed model provider key (or null if default).
 * @param routingMode The active routing strategy (DETERMINISTIC or AGENTIC).
 */
public record PromptContext(
    String rawUserQuery,
    Map<String, Object> userMetadata,
    Map<String, Object> systemMetadata,
    String refinedPrompt,
    String routedProvider,
    RoutingMode routingMode) {

  /**
   * Compact constructor enforcing immutability and defensive copying.
   *
   * @param rawUserQuery The original raw user query prompt.
   * @param userMetadata Immutable map containing enriched user attributes and preferences.
   * @param systemMetadata Immutable map containing enriched system and environment signals.
   * @param refinedPrompt The refined instruction text (defaults to rawUserQuery).
   * @param routedProvider The routed model provider key (or null if default).
   * @param routingMode The active routing strategy (DETERMINISTIC or AGENTIC).
   */
  public PromptContext {
    userMetadata = sanitizeMap(userMetadata);
    systemMetadata = sanitizeMap(systemMetadata);
    if (routingMode == null) {
      routingMode = RoutingMode.DETERMINISTIC;
    }
  }

  /**
   * Overloaded constructor for initial creation of PromptContext.
   *
   * @param rawUserQuery The raw user query prompt.
   * @param userMetadata Initial user context matrix.
   */
  public PromptContext(String rawUserQuery, Map<String, Object> userMetadata) {
    this(rawUserQuery, userMetadata, Map.of(), rawUserQuery, null, RoutingMode.DETERMINISTIC);
  }

  /**
   * Returns a copy of this context with a new refined prompt.
   *
   * @param refinedPrompt The refined prompt text.
   * @return A new PromptContext instance.
   */
  public PromptContext withRefinedPrompt(String refinedPrompt) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider, routingMode);
  }

  /**
   * Returns a copy of this context with a new routed provider.
   *
   * @param routedProvider The routed provider key.
   * @return A new PromptContext instance.
   */
  public PromptContext withRoutedProvider(String routedProvider) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider, routingMode);
  }

  /**
   * Returns a copy of this context with a new routing mode.
   *
   * @param routingMode The routing mode strategy.
   * @return A new PromptContext instance.
   */
  public PromptContext withRoutingMode(RoutingMode routingMode) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider, routingMode);
  }

  /**
   * Returns a copy of this context with a new user metadata map.
   *
   * @param userMetadata The new user metadata map.
   * @return A new PromptContext instance.
   */
  public PromptContext withUserMetadata(Map<String, Object> userMetadata) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider, routingMode);
  }

  /**
   * Returns a copy of this context with a new system metadata map.
   *
   * @param systemMetadata The new system metadata map.
   * @return A new PromptContext instance.
   */
  public PromptContext withSystemMetadata(Map<String, Object> systemMetadata) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider, routingMode);
  }

  /**
   * Sanitizes a map by removing null keys and values, then wrapping it in an immutable copy.
   *
   * @param input The map to sanitize.
   * @return A new immutable map containing only non-null entries.
   */
  private static Map<String, Object> sanitizeMap(Map<String, Object> input) {
    if (input == null || input.isEmpty()) {
      return Map.of();
    }
    var clean = new HashMap<String, Object>();
    input.forEach(
        (k, v) -> {
          if (k != null && v != null) {
            clean.put(k, v);
          }
        });
    return Map.copyOf(clean);
  }
}
