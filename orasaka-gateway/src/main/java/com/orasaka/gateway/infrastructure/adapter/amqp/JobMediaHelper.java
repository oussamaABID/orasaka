package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.gateway.infrastructure.support.PathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared utility for saving generated media files to the upload directory.
 *
 * <p>Centralizes the file-writing logic previously duplicated across JobListener strategies.
 *
 */
final class JobMediaHelper {

  private static final Logger logger = LoggerFactory.getLogger(JobMediaHelper.class);

  private JobMediaHelper() {}

  /**
   * Saves media bytes to the user's job output directory and returns the URL path.
   *
   * @param uploadDir The base upload directory.
   * @param userId The user ID.
   * @param jobId The job ID.
   * @param data The raw media bytes.
   * @param filename The output filename (e.g. "video.mp4", "image.png").
   * @return The relative URL path, or empty string if data is null/empty.
   */
  static String saveMediaToFile(
      String uploadDir, String userId, String jobId, byte[] data, String filename) {
    if (data == null || data.length == 0) {
      return "";
    }
    try {
      Path dir = PathResolver.resolve(uploadDir).resolve(userId).resolve(jobId).resolve("output");
      Files.createDirectories(dir);

      Path filePath = dir.resolve(filename);
      Files.write(filePath, data);

      logger.info("Saved generated media file to: {}", filePath);
      return "/uploads/" + userId + "/" + jobId + "/output/" + filename;
    } catch (IOException e) {
      logger.error("Failed to save generated media to file", e);
      return "";
    }
  }
}
