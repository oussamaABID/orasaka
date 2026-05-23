package com.orasaka.core.graph;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe runtime store managing lock status for capabilities under Project Loom. */
public class OrasakaAdminRegistry {

  /** Holds lock audit info. */
  public record LockInfo(String reason, LocalDateTime lockedAt) {
    public LockInfo {
      Objects.requireNonNull(reason, "reason cannot be null");
      Objects.requireNonNull(lockedAt, "lockedAt cannot be null");
    }
  }

  private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();

  /** Locks a capability node with a specified reason. */
  public void lock(String featureId, String reason) {
    Objects.requireNonNull(featureId, "Feature ID cannot be null");
    Objects.requireNonNull(reason, "Lock reason cannot be null");
    locks.put(featureId, new LockInfo(reason, LocalDateTime.now()));
  }

  /** Unlocks a capability node. */
  public void unlock(String featureId) {
    Objects.requireNonNull(featureId, "Feature ID cannot be null");
    locks.remove(featureId);
  }

  /** Resolves lock details for a capability node. */
  public Optional<LockInfo> getLock(String featureId) {
    Objects.requireNonNull(featureId, "Feature ID cannot be null");
    return Optional.ofNullable(locks.get(featureId));
  }
}
