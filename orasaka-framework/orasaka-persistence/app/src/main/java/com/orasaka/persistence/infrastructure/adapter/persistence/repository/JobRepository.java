package com.orasaka.persistence.infrastructure.adapter.persistence.repository;

import com.orasaka.persistence.infrastructure.adapter.persistence.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link JobEntity}. */
@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
  Page<JobEntity> findByUserId(String userId, Pageable pageable);

  void deleteByUserId(String userId);
}
