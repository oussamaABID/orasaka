package com.orasaka.core.pipeline;

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
 */
public record PromptContext(
    String rawUserQuery,
    Map<String, Object> userMetadata,
    Map<String, Object> systemMetadata,
    String refinedPrompt,
    String routedProvider) {

  /** Compact constructor enforcing immutability and defensive copying. */
  public PromptContext {
    userMetadata =
        java.util.Optional.ofNullable(userMetadata)
            .map(
                m -> {
                  var clean = new java.util.HashMap<String, Object>();
                  m.forEach(
                      (k, v) -> {
                        if (k != null && v != null) {
                          clean.put(k, v);
                        }
                      });
                  return Map.copyOf(clean);
                })
            .orElseGet(Map::of);
    systemMetadata =
        java.util.Optional.ofNullable(systemMetadata)
            .map(
                m -> {
                  var clean = new java.util.HashMap<String, Object>();
                  m.forEach(
                      (k, v) -> {
                        if (k != null && v != null) {
                          clean.put(k, v);
                        }
                      });
                  return Map.copyOf(clean);
                })
            .orElseGet(Map::of);
  }

  /**
   * Overloaded constructor for initial creation of PromptContext.
   *
   * @param rawUserQuery The raw user query prompt.
   * @param userMetadata Initial user context matrix.
   */
  public PromptContext(String rawUserQuery, Map<String, Object> userMetadata) {
    this(rawUserQuery, userMetadata, Map.of(), rawUserQuery, null);
  }

  /**
   * Returns a copy of this context with a new refined prompt.
   *
   * @param refinedPrompt The refined prompt text.
   * @return A new PromptContext instance.
   */
  public PromptContext withRefinedPrompt(String refinedPrompt) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider);
  }

  /**
   * Returns a copy of this context with a new routed provider.
   *
   * @param routedProvider The routed provider key.
   * @return A new PromptContext instance.
   */
  public PromptContext withRoutedProvider(String routedProvider) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider);
  }

  /**
   * Returns a copy of this context with a new user metadata map.
   *
   * @param userMetadata The new user metadata map.
   * @return A new PromptContext instance.
   */
  public PromptContext withUserMetadata(Map<String, Object> userMetadata) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider);
  }

  /**
   * Returns a copy of this context with a new system metadata map.
   *
   * @param systemMetadata The new system metadata map.
   * @return A new PromptContext instance.
   */
  public PromptContext withSystemMetadata(Map<String, Object> systemMetadata) {
    return new PromptContext(
        rawUserQuery, userMetadata, systemMetadata, refinedPrompt, routedProvider);
  }
}
