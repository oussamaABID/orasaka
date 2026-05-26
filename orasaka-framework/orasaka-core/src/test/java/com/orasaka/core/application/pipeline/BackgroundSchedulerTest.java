package com.orasaka.core.application.pipeline;

import static org.mockito.Mockito.*;

import com.orasaka.core.domain.ports.outbound.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackgroundSchedulerTest {

  @Mock private ToolRegistry toolRegistry;

  private BackgroundScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new BackgroundScheduler(toolRegistry);
  }

  @Test
  void executeIngestion_whenRagDisabled_doesNotTrigger() {
    when(toolRegistry.isRagIngestionEnabled()).thenReturn(false);

    scheduler.executeIngestion();

    verify(toolRegistry, never()).triggerIngestion();
  }

  @Test
  void executeIngestion_whenRagEnabled_triggersIngestion() {
    when(toolRegistry.isRagIngestionEnabled()).thenReturn(true);

    scheduler.executeIngestion();

    verify(toolRegistry).triggerIngestion();
  }

  @Test
  void executeIngestion_whenIngestionThrows_doesNotPropagateException() {
    when(toolRegistry.isRagIngestionEnabled()).thenReturn(true);
    doThrow(new RuntimeException("DB error")).when(toolRegistry).triggerIngestion();

    scheduler.executeIngestion();

    verify(toolRegistry).triggerIngestion();
  }
}
