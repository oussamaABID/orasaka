package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.domain.model.Context;
import java.util.Map;

/**
 * Strategy interface for feature-specific job execution logic.
 *
 * <p>Each implementation handles a single feature key (e.g., video analysis, image generation,
 * speech synthesis). The {@link JobListener} delegates to the appropriate strategy based on the
 * {@link JobMessage#featureKey()}.
 *
 */
sealed interface JobExecutionStrategy
    permits VideoAnalysisStrategy,
        VisionAnalysisStrategy,
        AudioAnalysisStrategy,
        VideoGenerationStrategy,
        ImageGenerationStrategy,
        SpeechSynthesisStrategy,
        ChatGenerationStrategy {

  /**
   * Determines whether this strategy handles the given feature key.
   *
   * @param featureKey The feature key from the job message.
   * @return {@code true} if this strategy should handle the job.
   */
  boolean supports(String featureKey);

  /**
   * Executes the job and returns the result payload.
   *
   * @param message The incoming job message.
   * @param context The fully resolved user security and preferences context.
   * @return The result map to be persisted.
   * @throws JobExecutionException If execution fails.
   */
  Map<String, Object> execute(JobMessage message, Context context) throws JobExecutionException;
}
