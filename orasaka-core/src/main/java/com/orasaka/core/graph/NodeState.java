package com.orasaka.core.graph;

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
  @JsonSubTypes.Type(value = Active.class, name = "ACTIVE"),
  @JsonSubTypes.Type(value = Locked.class, name = "LOCKED"),
  @JsonSubTypes.Type(value = Invisible.class, name = "INVISIBLE")
})
public sealed interface NodeState permits Active, Locked, Invisible {}
