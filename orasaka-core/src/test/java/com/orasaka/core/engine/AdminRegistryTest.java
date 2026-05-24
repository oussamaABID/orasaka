package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AdminRegistry} — thread-safe lock/unlock/query lifecycle. */
class AdminRegistryTest {

  private AdminRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new AdminRegistry();
  }

  @Nested
  @DisplayName("Lock lifecycle")
  class LockLifecycle {

    @Test
    @DisplayName("lock stores LockInfo retrievable via getLock")
    void lockAndGet() {
      registry.lock("orasaka.core.chat.image", "Maintenance window");
      var lock = registry.getLock("orasaka.core.chat.image");
      assertTrue(lock.isPresent());
      assertEquals("Maintenance window", lock.get().reason());
      assertNotNull(lock.get().lockedAt());
    }

    @Test
    @DisplayName("unlock removes lock")
    void unlockRemoves() {
      registry.lock("orasaka.core.chat.image", "Maintenance");
      registry.unlock("orasaka.core.chat.image");
      assertTrue(registry.getLock("orasaka.core.chat.image").isEmpty());
    }

    @Test
    @DisplayName("getLock returns empty for unlocked feature")
    void getUnlocked() {
      assertTrue(registry.getLock("orasaka.core.chat.text").isEmpty());
    }

    @Test
    @DisplayName("unlock non-existent feature is no-op")
    void unlockNonExistent() {
      assertDoesNotThrow(() -> registry.unlock("non.existent"));
    }
  }

  @Nested
  @DisplayName("Null safety")
  class NullSafety {

    @Test
    @DisplayName("lock with null featureId throws NPE")
    void lockNullId() {
      assertThrows(NullPointerException.class, () -> registry.lock(null, "reason"));
    }

    @Test
    @DisplayName("lock with null reason throws NPE")
    void lockNullReason() {
      assertThrows(NullPointerException.class, () -> registry.lock("id", null));
    }

    @Test
    @DisplayName("unlock with null throws NPE")
    void unlockNull() {
      assertThrows(NullPointerException.class, () -> registry.unlock(null));
    }

    @Test
    @DisplayName("getLock with null throws NPE")
    void getLockNull() {
      assertThrows(NullPointerException.class, () -> registry.getLock(null));
    }
  }

  @Nested
  @DisplayName("LockInfo record")
  class LockInfoTests {

    @Test
    @DisplayName("null reason throws NPE")
    void nullReason() {
      assertThrows(
          NullPointerException.class,
          () -> new AdminRegistry.LockInfo(null, java.time.LocalDateTime.now()));
    }

    @Test
    @DisplayName("null lockedAt throws NPE")
    void nullLockedAt() {
      assertThrows(NullPointerException.class, () -> new AdminRegistry.LockInfo("reason", null));
    }
  }
}
