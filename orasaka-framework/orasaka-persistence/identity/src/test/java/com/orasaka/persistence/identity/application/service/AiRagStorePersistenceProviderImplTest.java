package com.orasaka.persistence.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.persistence.identity.domain.model.AiRagStoreDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiRagStoreEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.AiRagStoreRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiRagStorePersistenceProviderImplTest {

  private AiRagStoreRepository repository;
  private AiRagStorePersistenceProviderImpl provider;

  @BeforeEach
  void setUp() {
    repository = mock(AiRagStoreRepository.class);
    provider = new AiRagStorePersistenceProviderImpl(repository);
  }

  @Test
  void testConstructorNullCheck() {
    assertThatThrownBy(() -> new AiRagStorePersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("AiRagStoreRepository cannot be null");
  }

  @Test
  void testFindByUserIdAndEnabledTrue() {
    AiRagStoreEntity entity = new AiRagStoreEntity();
    entity.setId(1);
    entity.setUserId("user123");
    entity.setName("test-store");
    entity.setStoreType("pgvector");
    entity.setHost("localhost");
    entity.setPort(5432);
    entity.setDatabaseName("orasaka_db");
    entity.setTableName("vectors");
    entity.setUsername("admin");
    entity.setPassword("pass");
    entity.setEnabled(true);

    when(repository.findByUserIdAndEnabledTrue("user123")).thenReturn(List.of(entity));

    List<AiRagStoreDto> result = provider.findByUserIdAndEnabledTrue("user123");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).userId()).isEqualTo("user123");
    assertThat(result.get(0).name()).isEqualTo("test-store");
  }

  @Test
  void testFindByUserIdNullCheck() {
    assertThatThrownBy(() -> provider.findByUserIdAndEnabledTrue(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");
  }

  @Test
  void testSave() {
    AiRagStoreDto dto =
        new AiRagStoreDto(
            1,
            "user123",
            "test-store",
            "pgvector",
            "localhost",
            5432,
            "orasaka_db",
            "vectors",
            "admin",
            "pass",
            true,
            java.time.Instant.now());

    AiRagStoreEntity entity = new AiRagStoreEntity();
    entity.setId(1);
    entity.setUserId("user123");
    entity.setName("test-store");
    entity.setStoreType("pgvector");
    entity.setHost("localhost");
    entity.setPort(5432);
    entity.setDatabaseName("orasaka_db");
    entity.setTableName("vectors");
    entity.setUsername("admin");
    entity.setPassword("pass");
    entity.setEnabled(true);

    when(repository.save(any(AiRagStoreEntity.class))).thenReturn(entity);

    AiRagStoreDto savedDto = provider.save(dto);
    assertThat(savedDto).isNotNull();
    assertThat(savedDto.userId()).isEqualTo("user123");
    assertThat(savedDto.name()).isEqualTo("test-store");
  }

  @Test
  void testSaveNullCheck() {
    assertThatThrownBy(() -> provider.save(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("AiRagStoreDto cannot be null");
  }

  @Test
  void testDeleteById() {
    provider.deleteById(1);
    verify(repository).deleteById(1);
  }

  @Test
  void testDeleteByIdNullCheck() {
    assertThatThrownBy(() -> provider.deleteById(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ID cannot be null");
  }
}
