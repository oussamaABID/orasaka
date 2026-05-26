package com.orasaka.gateway.infrastructure.support;

import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.persistence.domain.ports.inbound.JobPersistenceProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private utility centralizing the job submission lifecycle.
 *
 * <p>Eliminates the 3× duplicated pattern of:
 *
 * <ol>
 *   <li>Generating a job ID
 *   <li>Creating input/output directories
 *   <li>Copying the source file to the job's input directory
 *   <li>Persisting the job record
 *   <li>Publishing the job message to the queue
 * </ol>
 *
 * @since 1.1.0
 */
public final class JobSubmissionHelper {

  private JobSubmissionHelper() {}

  /**
   * Result record returned after successful job submission.
   *
   * @param jobId the generated unique job identifier
   * @param inputFilePath the absolute path to the copied input file
   */
  public record SubmissionResult(String jobId, Path inputFilePath) {}

  /**
   * Submits a media processing job: generates ID, creates directories, copies the source file,
   * persists the record, and publishes to the message queue.
   *
   * @param jobPersistenceProvider the persistence provider for job record creation
   * @param jobQueuePublisher the queue publisher for async job dispatch
   * @param uploadDir the root upload directory
   * @param userId the user ID
   * @param featureKey the job type feature key (e.g. {@code "orasaka.core.media.vision"})
   * @param payload the job payload map
   * @param sourceFile the source file to copy into the job's input directory
   * @return the submission result containing the job ID and input file path
   * @throws IOException if directory creation or file copy fails
   */
  public static SubmissionResult submit(
      JobPersistenceProvider jobPersistenceProvider,
      JobQueuePublisherService jobQueuePublisher,
      String uploadDir,
      String userId,
      String featureKey,
      Map<String, Object> payload,
      File sourceFile)
      throws IOException {

    String jobId = UUID.randomUUID().toString();

    Path jobInputDir = Paths.get(uploadDir, userId, jobId, "input");
    Path jobOutputDir = Paths.get(uploadDir, userId, jobId, "output");
    Path jobTempDir = Paths.get(uploadDir, userId, jobId, "temp");
    Files.createDirectories(jobInputDir);
    Files.createDirectories(jobOutputDir);
    Files.createDirectories(jobTempDir);

    Path targetPath = jobInputDir.resolve(sourceFile.getName());
    Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

    payload.put("filePath", targetPath.toAbsolutePath().toString());

    // Detect and inject the input file MIME type
    String mimeType = Files.probeContentType(targetPath);
    if (mimeType == null) {
      String filename = targetPath.getFileName().toString().toLowerCase();
      if (filename.endsWith(".mp4")) {
        mimeType = "video/mp4";
      } else if (filename.endsWith(".mp3")) {
        mimeType = "audio/mpeg";
      } else if (filename.endsWith(".png")) {
        mimeType = "image/png";
      } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
        mimeType = "image/jpeg";
      } else {
        mimeType = "application/octet-stream";
      }
    }
    payload.put("mimeType", mimeType);

    jobPersistenceProvider.createJob(jobId, userId, featureKey, payload);

    JobMessage message = new JobMessage(jobId, userId, featureKey, payload);
    jobQueuePublisher.publish(message);

    return new SubmissionResult(jobId, targetPath);
  }
}
