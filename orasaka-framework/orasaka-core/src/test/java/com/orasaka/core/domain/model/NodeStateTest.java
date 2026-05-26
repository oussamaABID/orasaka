package com.orasaka.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NodeStateTest {

  @Test
  void active_isNodeState() {
    var active = new NodeState.Active();
    assertInstanceOf(NodeState.class, active);
  }

  @Test
  void invisible_isNodeState() {
    var invisible = new NodeState.Invisible();
    assertInstanceOf(NodeState.class, invisible);
  }

  @Test
  void locked_mapsAllFields() {
    LocalDateTime now = LocalDateTime.now();
    var locked = new NodeState.Locked("Maintenance", now);
    assertEquals("Maintenance", locked.reason());
    assertEquals(now, locked.lockedAt());
  }

  @Test
  void locked_nullReason_throws() {
    var now = LocalDateTime.now();
    assertThrows(NullPointerException.class, () -> new NodeState.Locked(null, now));
  }

  @Test
  void locked_blankReason_throws() {
    var now = LocalDateTime.now();
    assertThrows(IllegalArgumentException.class, () -> new NodeState.Locked("  ", now));
  }

  @Test
  void locked_nullLockedAt_throws() {
    assertThrows(NullPointerException.class, () -> new NodeState.Locked("reason", null));
  }

  @Test
  void sealedInterface_patternMatch() {
    NodeState state = new NodeState.Active();
    String label =
        switch (state) {
          case NodeState.Active a -> "active";
          case NodeState.Locked l -> "locked";
          case NodeState.Invisible i -> "invisible";
        };
    assertEquals("active", label);
  }
}
