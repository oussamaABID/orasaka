package com.orasaka.core.graph;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a temporarily disabled or locked capability state in the Orasaka Operation Graph, with
 * structured reason and audit timestamp.
 *
 * @param reason The contextual explanation for why the operation was locked.
 * @param lockedAt The audit timestamp when the lock was applied.
 */
public final record Locked(String reason, LocalDateTime lockedAt) implements NodeState {
  public Locked {
    Objects.requireNonNull(reason, "Lock reason cannot be null");
    if (reason.isBlank()) {
      throw new IllegalArgumentException("Lock reason cannot be blank");
    }
    Objects.requireNonNull(lockedAt, "Lock audit timestamp cannot be null");
  }
}
