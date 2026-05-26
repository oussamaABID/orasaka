package com.orasaka.core.application.service;

import com.orasaka.core.domain.event.JobStatusChangedEvent;
import com.orasaka.core.domain.model.job.JobInfo;
import com.orasaka.core.domain.model.job.JobStatus;
import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.persistence.domain.ports.inbound.JobPersistenceProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Package-private implementation of the JobService inbound port. Delegates to the out-of-boundary
 * persistence package. Follows ERR-105 (Interface-Driven Boundaries).
 */
@Service
class JobServiceImpl implements JobService {

  private final JobPersistenceProvider jobPersistenceProvider;
  private final ApplicationEventPublisher eventPublisher;

  JobServiceImpl(
      JobPersistenceProvider jobPersistenceProvider, ApplicationEventPublisher eventPublisher) {
    this.jobPersistenceProvider =
        Objects.requireNonNull(jobPersistenceProvider, "JobPersistenceProvider cannot be null");
    this.eventPublisher =
        Objects.requireNonNull(eventPublisher, "ApplicationEventPublisher cannot be null");
  }

  @Override
  public String createJob(String userId, String featureKey, Map<String, Object> payload) {
    String jobId = jobPersistenceProvider.createJob(userId, featureKey, payload);
    publishEvent(jobId);
    return jobId;
  }

  @Override
  public String createJob(
      String jobId, String userId, String featureKey, Map<String, Object> payload) {
    String createdId = jobPersistenceProvider.createJob(jobId, userId, featureKey, payload);
    publishEvent(createdId);
    return createdId;
  }

  @Override
  public void updateJobStatus(
      String jobId, JobStatus status, Map<String, Object> result, String errorMessage) {
    jobPersistenceProvider.updateJobStatus(jobId, status.name(), result, errorMessage);
    publishEvent(jobId);
  }

  @Override
  public Optional<JobInfo> getJob(String id) {
    return jobPersistenceProvider.getJob(id).map(JobInfoMapper::toInfo);
  }

  @Override
  public Page<JobInfo> getJobsByUserId(String userId, Pageable pageable) {
    return jobPersistenceProvider.getJobsByUserId(userId, pageable).map(JobInfoMapper::toInfo);
  }

  @Override
  public Page<JobInfo> getAllJobs(Pageable pageable) {
    return jobPersistenceProvider.getAllJobs(pageable).map(JobInfoMapper::toInfo);
  }

  @Override
  public void purgeJobsByUserId(String userId) {
    jobPersistenceProvider.purgeJobsByUserId(userId);
  }

  private void publishEvent(String jobId) {
    getJob(jobId)
        .ifPresent(jobInfo -> eventPublisher.publishEvent(new JobStatusChangedEvent(jobInfo)));
  }
}
