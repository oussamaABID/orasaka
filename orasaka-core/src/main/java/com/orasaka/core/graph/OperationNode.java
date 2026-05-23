package com.orasaka.core.graph;

import java.util.Objects;

/**
 * Defines a capability node inside the Orasaka Operation Graph.
 *
 * @param id The unique identifier of the operation node (e.g., "orasaka.core.chat.image").
 * @param label The user-facing label text.
 * @param icon The visual icon keyword.
 * @param presentationContext The display context (defaults to "CONTEXT_MENU_PLUS").
 * @param state The current runtime state of the capability.
 * @param executionDetails The HTTP execution parameters to invoke this node.
 */
public final record OperationNode(
    String id,
    String label,
    String icon,
    String presentationContext,
    NodeState state,
    TargetExecutionUri executionDetails) {
  public OperationNode {
    Objects.requireNonNull(id, "Operation Node ID cannot be null");
    if (id.isBlank()) {
      throw new IllegalArgumentException("Operation Node ID cannot be blank");
    }
    Objects.requireNonNull(label, "Operation Node label cannot be null");
    if (label.isBlank()) {
      throw new IllegalArgumentException("Operation Node label cannot be blank");
    }
    Objects.requireNonNull(icon, "Operation Node icon cannot be null");
    if (icon.isBlank()) {
      throw new IllegalArgumentException("Operation Node icon cannot be blank");
    }
    if (presentationContext == null || presentationContext.isBlank()) {
      presentationContext = "CONTEXT_MENU_PLUS";
    }
    Objects.requireNonNull(state, "Operation Node state cannot be null");
    Objects.requireNonNull(executionDetails, "Operation Node executionDetails cannot be null");
  }
}
