package com.orasaka.core.interceptors.rag;

import com.orasaka.core.interceptors.tool.OrasakaToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled background task processor for RAG ingestion pipelines. */
@Component
public class OrasakaBackgroundScheduler {

  private static final Logger log = LoggerFactory.getLogger(OrasakaBackgroundScheduler.class);
  private static final String CRON_EXPRESSION = "${orasaka.tools.rag.cron:0 0 3 * * ?}";

  private final OrasakaToolRegistry toolRegistry;

  /**
   * Initializes the scheduler.
   *
   * @param toolRegistry The active {@link OrasakaToolRegistry} implementation.
   */
  public OrasakaBackgroundScheduler(OrasakaToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  /** Triggers asynchronous RAG ingestion daily at 3:00 AM. */
  @Scheduled(cron = CRON_EXPRESSION)
  public void executeIngestion() {
    long startTime = System.nanoTime();

    // Passivity: Immediately abort if RAG ingestion is not enabled for any tool.
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
