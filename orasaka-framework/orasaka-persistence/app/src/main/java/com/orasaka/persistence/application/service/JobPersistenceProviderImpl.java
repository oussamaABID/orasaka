package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.event.JobStatusChangedEvent;
import com.orasaka.persistence.domain.model.JobDto;
import com.orasaka.persistence.domain.ports.inbound.JobPersistenceProvider;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.JobEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.JobRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of the JobPersistenceProvider. */
@Service
@Transactional
class JobPersistenceProviderImpl implements JobPersistenceProvider {

  private final JobRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  JobPersistenceProviderImpl(JobRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = Objects.requireNonNull(repository, "JobRepository cannot be null");
    this.eventPublisher =
        Objects.requireNonNull(eventPublisher, "ApplicationEventPublisher cannot be null");
  }

  @Override
  public String createJob(String userId, String featureKey, Map<String, Object> payload) {
    return createJob(null, userId, featureKey, payload);
  }

  @Override
  public String createJob(
      String jobId, String userId, String featureKey, Map<String, Object> payload) {
    Objects.requireNonNull(featureKey, "Feature key cannot be null");
    String id = (jobId == null || jobId.isBlank()) ? UUID.randomUUID().toString() : jobId;
    JobEntity entity = new JobEntity();
    entity.setId(id);
    entity.setUserId(userId);
    entity.setFeatureKey(featureKey);
    entity.setStatus("PENDING");
    entity.setPayload(payload);

    Instant now = Instant.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    repository.save(entity);
    eventPublisher.publishEvent(new JobStatusChangedEvent(JobMapper.toDto(entity)));
    return id;
  }

  @Override
  public void updateJobStatus(
      String jobId, String status, Map<String, Object> result, String errorMessage) {
    Objects.requireNonNull(jobId, "Job ID cannot be null");
    Objects.requireNonNull(status, "Status cannot be null");

    JobEntity entity =
        repository
            .findById(jobId)
            .orElseThrow(
                () -> new IllegalArgumentException("Job with ID " + jobId + " does not exist"));

    entity.setStatus(status);
    if (result != null) {
      entity.setResult(result);
    }
    entity.setErrorMessage(errorMessage);
    entity.setUpdatedAt(Instant.now());

    repository.save(entity);
    eventPublisher.publishEvent(new JobStatusChangedEvent(JobMapper.toDto(entity)));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<JobDto> getJob(String jobId) {
    Objects.requireNonNull(jobId, "Job ID cannot be null");
    return repository.findById(jobId).map(JobMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<JobDto> getJobsByUserId(String userId, Pageable pageable) {
    Objects.requireNonNull(userId, "User ID cannot be null");
    Objects.requireNonNull(pageable, "Pageable cannot be null");
    return repository.findByUserId(userId, pageable).map(JobMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<JobDto> getAllJobs(Pageable pageable) {
    Objects.requireNonNull(pageable, "Pageable cannot be null");
    return repository.findAll(pageable).map(JobMapper::toDto);
  }

  @Override
  public void purgeJobsByUserId(String userId) {
    Objects.requireNonNull(userId, "User ID cannot be null");
    repository.deleteByUserId(userId);
  }
}
