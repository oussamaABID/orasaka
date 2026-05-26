package com.orasaka.core.application.pipeline;

import com.orasaka.core.domain.ports.outbound.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled background task processor for RAG ingestion pipelines.
 *
 * <p>This component triggers RAG ingestion at a configurable time interval, allowing for periodic
 * updates of the vector database without user intervention.
 */
@Component
public class BackgroundScheduler {

  /** Logger instance for logging RAG ingestion events. */
  private static final Logger log = LoggerFactory.getLogger(BackgroundScheduler.class);

  /** Cron expression for scheduling RAG ingestion, defaulting to 3:00 AM daily. */
  private static final String CRON_EXPRESSION = "${orasaka.tools.rag.cron:0 0 3 * * ?}";

  /** The active {@link ToolRegistry} implementation. */
  private final ToolRegistry toolRegistry;

  /**
   * Initializes the scheduler.
   *
   * @param toolRegistry The active {@link ToolRegistry} implementation.
   */
  public BackgroundScheduler(ToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  /**
   * Triggers asynchronous RAG ingestion daily at 3:00 AM. The cron expression is configurable via
   * the {@code orasaka.tools.rag.cron} property, with a default value of "0 0 3 * * ?" (3:00 AM
   * daily).
   */
  @Scheduled(cron = CRON_EXPRESSION)
  public void executeIngestion() {
    long startTime = System.nanoTime();

    if (!toolRegistry.isRagIngestionEnabled()) {
      long durationNs = System.nanoTime() - startTime;
      double durationMs = durationNs / 1_000_000.0;
      log.debug(
          "Orasaka RAG ingestion aborted (passive bypass). Check duration: {} ms", durationMs);
      return;
    }

    log.info("Starting Orasaka RAG background ingestion...");
    try {
      toolRegistry.triggerIngestion();
      long durationNs = System.nanoTime() - startTime;
      double durationMs = durationNs / 1_000_000.0;
      log.info("Orasaka RAG background ingestion completed in {} ms.", durationMs);
    } catch (Exception e) {
      log.error("Failed to run background RAG ingestion", e);
    }
  }
}
