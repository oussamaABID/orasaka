package com.orasaka.persistence.domain.ports.inbound;

import com.orasaka.persistence.domain.model.JobDto;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Public contract for job lifecycle management and state persistence. */
public interface JobPersistenceProvider {

  /**
   * Instantiates and persists a new job in PENDING status.
   *
   * @param userId The authenticated user ID.
   * @param featureKey The operation capability feature key.
   * @param payload The request parameters payload.
   * @return The generated job ID.
   */
  String createJob(String userId, String featureKey, Map<String, Object> payload);

  /**
   * Instantiates and persists a new job in PENDING status with a custom job ID.
   *
   * @param jobId The custom job ID to use, or null to auto-generate.
   * @param userId The authenticated user ID.
   * @param featureKey The operation capability feature key.
   * @param payload The request parameters payload.
   * @return The job ID.
   */
  String createJob(String jobId, String userId, String featureKey, Map<String, Object> payload);

  /**
   * Updates the execution status, result payload, or error of a job.
   *
   * @param jobId The target job ID.
   * @param status The new status (e.g. PROCESSING, COMPLETED, FAILED).
   * @param result The execution results.
   * @param errorMessage The failure reason if the job failed.
   */
  void updateJobStatus(
      String jobId, String status, Map<String, Object> result, String errorMessage);

  /**
   * Retrieves the current state snapshot of a job.
   *
   * @param jobId The job ID to resolve.
   * @return Optional containing the JobDto if found.
   */
  Optional<JobDto> getJob(String jobId);

  /**
   * Retrieves a paginated list of all jobs belonging to the authenticated user.
   *
   * @param userId The authenticated user ID.
   * @param pageable The pagination details.
   * @return Paginated list of JobDtos.
   */
  Page<JobDto> getJobsByUserId(String userId, Pageable pageable);

  /**
   * Retrieves a paginated list of all jobs across all users in the system.
   *
   * @param pageable The pagination details.
   * @return Paginated list of all JobDtos.
   */
  Page<JobDto> getAllJobs(Pageable pageable);

  /**
   * Purges all job records associated with a specific user.
   *
   * @param userId The ID of the user whose jobs are to be deleted.
   */
  void purgeJobsByUserId(String userId);
}
