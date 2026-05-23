package com.orasaka.core.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed interface representing the polymorphic state of a graph node in the Orasaka Operation
 * Graph.
 *
 * <p>Adheres strictly to the pattern-matching switch-expression mandates of [ERR-107].
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = NodeState.Active.class, name = "ACTIVE"),
  @JsonSubTypes.Type(value = NodeState.Locked.class, name = "LOCKED"),
  @JsonSubTypes.Type(value = NodeState.Invisible.class, name = "INVISIBLE")
})
public sealed interface NodeState permits NodeState.Active, NodeState.Locked, NodeState.Invisible {

  record Active() implements NodeState {}

  record Locked(String reason, java.time.LocalDateTime lockedAt) implements NodeState {
    public Locked {
      java.util.Objects.requireNonNull(reason, "Lock reason cannot be null");
      if (reason.isBlank()) {
        throw new IllegalArgumentException("Lock reason cannot be blank");
      }
      java.util.Objects.requireNonNull(lockedAt, "Lock audit timestamp cannot be null");
    }
  }

  record Invisible() implements NodeState {}
}
