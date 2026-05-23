package com.orasaka.core.orchestration;

import java.util.HashMap;
import java.util.Map;

/** State machine context holding user and system matrices for prompt refinement and routing. */
public final class PromptContext {

  private final String rawUserQuery;
  private final Map<String, Object> userMetadata;
  private final Map<String, Object> systemMetadata;
  private String refinedPrompt;
  private String routedProvider;

  /**
   * Constructs a prompt context.
   *
   * @param rawUserQuery The original user query.
   * @param userMetadata Initial user attributes (e.g. from the token context).
   */
  public PromptContext(String rawUserQuery, Map<String, Object> userMetadata) {
    this.rawUserQuery = rawUserQuery;
    this.userMetadata = new HashMap<>(userMetadata != null ? userMetadata : Map.of());
    this.systemMetadata = new HashMap<>();
    this.refinedPrompt = rawUserQuery;
  }

  /**
   * Gets the raw user query.
   *
   * @return The raw query string.
   */
  public String rawUserQuery() {
    return rawUserQuery;
  }

  /**
   * Gets the user metadata map.
   *
   * @return Extensible map of user context attributes.
   */
  public Map<String, Object> userMetadata() {
    return userMetadata;
  }

  /**
   * Gets the system metadata map.
   *
   * @return Extensible map of system context attributes.
   */
  public Map<String, Object> systemMetadata() {
    return systemMetadata;
  }

  /**
   * Gets the refined prompt. Defaults to rawUserQuery.
   *
   * @return The refined instruction string.
   */
  public String refinedPrompt() {
    return refinedPrompt;
  }

  /**
   * Sets the refined prompt instruction.
   *
   * @param refinedPrompt The new refined instruction text.
   */
  public void setRefinedPrompt(String refinedPrompt) {
    this.refinedPrompt = refinedPrompt;
  }

  /**
   * Gets the routed provider selection.
   *
   * @return The provider key string, or null if default routing applies.
   */
  public String routedProvider() {
    return routedProvider;
  }

  /**
   * Sets the routed provider selection.
   *
   * @param routedProvider The provider key.
   */
  public void setRoutedProvider(String routedProvider) {
    this.routedProvider = routedProvider;
  }
}
