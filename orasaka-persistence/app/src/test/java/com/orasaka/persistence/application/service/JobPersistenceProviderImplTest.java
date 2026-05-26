package com.orasaka.persistence.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orasaka.persistence.domain.model.JobDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.JobEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.JobRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Unit tests for {@link JobPersistenceProviderImpl}. */
@ExtendWith(MockitoExtension.class)
class JobPersistenceProviderImplTest {

  @Mock private JobRepository repository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private JobPersistenceProviderImpl provider;

  @BeforeEach
  void setUp() {
    provider = new JobPersistenceProviderImpl(repository, eventPublisher);
  }

  private static JobEntity buildEntity(String id, String userId, String featureKey, String status) {
    Instant now = Instant.now();
    JobEntity entity = new JobEntity();
    entity.setId(id);
    entity.setUserId(userId);
    entity.setFeatureKey(featureKey);
    entity.setStatus(status);
    entity.setPayload(Map.of());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    return entity;
  }

  @Test
  void createJob_withNullId_generatesUuid() {
    when(repository.save(any(JobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    String id = provider.createJob(null, "user1", "image", Map.of("prompt", "test"));

    assertNotNull(id);
    assertFalse(id.isBlank());
    verify(repository).save(any(JobEntity.class));
    verify(eventPublisher).publishEvent(any(Object.class));
  }

  @Test
  void createJob_withExplicitId_usesProvidedId() {
    when(repository.save(any(JobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    String id = provider.createJob("my-id", "user1", "speech", Map.of());

    assertEquals("my-id", id);
  }

  @Test
  void createJob_noIdOverload_delegatesToFull() {
    when(repository.save(any(JobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    String id = provider.createJob("user1", "image", Map.of());

    assertNotNull(id);
  }

  @Test
  void createJob_nullFeatureKey_throwsNpe() {
    assertThrows(
        NullPointerException.class,
        () -> {
          provider.createJob("user1", null, Map.of());
        });
  }

  @Test
  void updateJobStatus_existingJob_updatesFields() {
    JobEntity entity = buildEntity("job-1", "user1", "image", "PENDING");
    when(repository.findById("job-1")).thenReturn(Optional.of(entity));
    when(repository.save(any(JobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    provider.updateJobStatus("job-1", "COMPLETED", Map.of("url", "/result"), null);

    assertEquals("COMPLETED", entity.getStatus());
    assertEquals("/result", entity.getResult().get("url"));
    verify(eventPublisher).publishEvent(any(Object.class));
  }

  @Test
  void updateJobStatus_nonexistentJob_throwsException() {
    when(repository.findById("nope")).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () -> provider.updateJobStatus("nope", "COMPLETED", null, null));
  }

  @Test
  void updateJobStatus_nullJobId_throwsNpe() {
    assertThrows(
        NullPointerException.class, () -> provider.updateJobStatus(null, "OK", null, null));
  }

  @Test
  void updateJobStatus_nullStatus_throwsNpe() {
    assertThrows(
        NullPointerException.class, () -> provider.updateJobStatus("id", null, null, null));
  }

  @Test
  void updateJobStatus_withErrorMessage_setsError() {
    JobEntity entity = buildEntity("job-err", "user1", "image", "RUNNING");
    when(repository.findById("job-err")).thenReturn(Optional.of(entity));
    when(repository.save(any(JobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    provider.updateJobStatus("job-err", "FAILED", null, "Timeout exceeded");

    assertEquals("FAILED", entity.getStatus());
    assertEquals("Timeout exceeded", entity.getErrorMessage());
  }

  @Test
  void getJob_existing_returnsDto() {
    JobEntity entity = buildEntity("job-1", "user1", "image", "COMPLETED");
    entity.setResult(Map.of("url", "/img.png"));
    when(repository.findById("job-1")).thenReturn(Optional.of(entity));

    Optional<JobDto> result = provider.getJob("job-1");

    assertTrue(result.isPresent());
    assertEquals("job-1", result.get().id());
    assertEquals("COMPLETED", result.get().status());
  }

  @Test
  void getJob_nonexistent_returnsEmpty() {
    when(repository.findById("none")).thenReturn(Optional.empty());

    assertTrue(provider.getJob("none").isEmpty());
  }

  @Test
  void getJob_nullId_throwsNpe() {
    assertThrows(NullPointerException.class, () -> provider.getJob(null));
  }

  @Test
  void getJobsByUserId_returnsPage() {
    JobEntity entity = buildEntity("j1", "user1", "image", "PENDING");
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findByUserId("user1", pageable)).thenReturn(new PageImpl<>(List.of(entity)));

    Page<JobDto> result = provider.getJobsByUserId("user1", pageable);

    assertEquals(1, result.getTotalElements());
  }

  @Test
  void getAllJobs_returnsPage() {
    Pageable pageable = PageRequest.of(0, 20);
    when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

    Page<JobDto> result = provider.getAllJobs(pageable);

    assertEquals(0, result.getTotalElements());
  }

  @Test
  void purgeJobsByUserId_delegatesToRepository() {
    provider.purgeJobsByUserId("user1");

    verify(repository).deleteByUserId("user1");
  }

  @Test
  void purgeJobsByUserId_nullUserId_throwsNpe() {
    assertThrows(NullPointerException.class, () -> provider.purgeJobsByUserId(null));
  }
}
