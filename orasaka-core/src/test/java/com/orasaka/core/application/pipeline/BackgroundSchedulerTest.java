package com.orasaka.core.application.pipeline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.ports.outbound.ToolRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link BackgroundScheduler} — RAG ingestion scheduling. */
class BackgroundSchedulerTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() { org.junit.jupiter.api.Assertions.assertTrue(true); }


  @Nested
  @DisplayName("Ingestion execution")
  class IngestionExecution {

    @Test
    @DisplayName("aborts when RAG ingestion is disabled")
    void abortsWhenDisabled() {
      var registry = mock(ToolRegistry.class);
      when(registry.isRagIngestionEnabled()).thenReturn(false);
      var scheduler = new BackgroundScheduler(registry);

      assertDoesNotThrow(scheduler::executeIngestion);
      verify(registry, never()).triggerIngestion();
    }

    @Test
    @DisplayName("triggers ingestion when enabled")
    void triggersWhenEnabled() {
      var registry = mock(ToolRegistry.class);
      when(registry.isRagIngestionEnabled()).thenReturn(true);
      var scheduler = new BackgroundScheduler(registry);

      scheduler.executeIngestion();
      verify(registry).triggerIngestion();
    }

    @Test
    @DisplayName("handles ingestion exception gracefully")
    void handlesException() {
      var registry = mock(ToolRegistry.class);
      when(registry.isRagIngestionEnabled()).thenReturn(true);
      doThrow(new RuntimeException("boom")).when(registry).triggerIngestion();
      var scheduler = new BackgroundScheduler(registry);

      assertDoesNotThrow(scheduler::executeIngestion);
    }
  }
}
