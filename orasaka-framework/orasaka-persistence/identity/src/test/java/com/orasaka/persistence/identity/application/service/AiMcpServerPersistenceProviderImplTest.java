package com.orasaka.persistence.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.persistence.identity.domain.model.AiMcpServerDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AiMcpServerEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.AiMcpServerRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiMcpServerPersistenceProviderImplTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  private AiMcpServerRepository repository;
  private AiMcpServerPersistenceProviderImpl provider;

  @BeforeEach
  void setUp() {
    repository = mock(AiMcpServerRepository.class);
    provider = new AiMcpServerPersistenceProviderImpl(repository);
  }

  @Test
  void testConstructorNullCheck() {
    assertThatThrownBy(() -> new AiMcpServerPersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("AiMcpServerRepository cannot be null");
  }

  @Test
  void testFindByUserIdAndEnabledTrue() {
    AiMcpServerEntity entity = new AiMcpServerEntity();
    entity.setId(1);
    entity.setUserId("user123");
    entity.setName("test-server");
    entity.setUrl("http://localhost");
    entity.setEnabled(true);

    when(repository.findByUserIdAndEnabledTrue("user123")).thenReturn(List.of(entity));

    List<AiMcpServerDto> result = provider.findByUserIdAndEnabledTrue("user123");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).userId()).isEqualTo("user123");
    assertThat(result.get(0).name()).isEqualTo("test-server");
  }

  @Test
  void testFindByUserIdNullCheck() {
    assertThatThrownBy(() -> provider.findByUserIdAndEnabledTrue(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");
  }

  @Test
  void testSave() {
    AiMcpServerDto dto =
        new AiMcpServerDto(
            1,
            "user123",
            "test-server",
            "http://localhost",
            true,
            java.time.Instant.now(FIXED_CLOCK));

    AiMcpServerEntity entity = new AiMcpServerEntity();
    entity.setId(1);
    entity.setUserId("user123");
    entity.setName("test-server");
    entity.setUrl("http://localhost");
    entity.setEnabled(true);

    when(repository.save(any(AiMcpServerEntity.class))).thenReturn(entity);

    AiMcpServerDto savedDto = provider.save(dto);
    assertThat(savedDto).isNotNull();
    assertThat(savedDto.userId()).isEqualTo("user123");
    assertThat(savedDto.name()).isEqualTo("test-server");
  }

  @Test
  void testSaveNullCheck() {
    assertThatThrownBy(() -> provider.save(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("AiMcpServerDto cannot be null");
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
