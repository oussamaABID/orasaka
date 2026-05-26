package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.domain.model.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Strategy interface for feature-specific job execution logic.
 *
 * <p>Each implementation handles a single feature key (e.g., video analysis, image generation,
 * speech synthesis). The {@link JobListener} delegates to the appropriate strategy based on the
 * {@link JobMessage#featureKey()}.
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

  /**
   * Extracts file bytes from the job message payload.
   *
   * <p>Validates that the payload contains a {@code filePath} field, that the referenced file
   * exists, and reads its contents. This eliminates the duplicated file-loading pattern across
   * media strategy implementations.
   *
   * @param message The job message containing the payload with filePath.
   * @param mediaType A label for error messages (e.g., "Audio", "Video", "Image").
   * @return The file contents as a byte array.
   * @throws JobExecutionException If filePath is missing, the file does not exist, or reading
   *     fails.
   */
  default byte[] extractFileBytes(JobMessage message, String mediaType)
      throws JobExecutionException {
    String filePath = (String) message.payload().get("filePath");
    if (filePath == null) {
      throw new JobExecutionException("Payload does not contain filePath field");
    }
    File file = new File(filePath);
    if (!file.exists()) {
      throw new JobExecutionException(mediaType + " file not found at " + filePath);
    }
    try {
      return Files.readAllBytes(file.toPath());
    } catch (IOException e) {
      throw new JobExecutionException(
          "Failed to read " + mediaType.toLowerCase() + " file: " + e.getMessage(), e);
    }
  }
}
