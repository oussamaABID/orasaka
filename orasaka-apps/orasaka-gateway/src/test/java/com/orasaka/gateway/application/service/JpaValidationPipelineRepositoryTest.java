package com.orasaka.gateway.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.ValidationPipelineConfiguration;
import com.orasaka.core.domain.model.ValidationStepType;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.ValidationPipelineConfigEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.ValidationPipelineConfigJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link JpaValidationPipelineRepository}. */
class JpaValidationPipelineRepositoryTest {

  private final ValidationPipelineConfigJpaRepository jpaRepository =
      mock(ValidationPipelineConfigJpaRepository.class);
  private final JpaValidationPipelineRepository repository =
      new JpaValidationPipelineRepository(jpaRepository);

  // ── findAllOrderedByExecution ────────────────────────────────────────────

  @Test
  @DisplayName("findAllOrderedByExecution returns mapped configs in order")
  void findAllOrdered_returnsMappedConfigs() {
    var entityA = buildEntity("STRUCTURAL_A", true, 1);
    var entityB = buildEntity("SANDBOX_B", true, 2);
    var entityC = buildEntity("SEMANTIC_C", false, 3);
    var entityD = buildEntity("TDR_D", false, 4);

    when(jpaRepository.findAllByOrderByExecutionOrderAsc())
        .thenReturn(List.of(entityA, entityB, entityC, entityD));

    List<ValidationPipelineConfiguration> result = repository.findAllOrderedByExecution();

    assertThat(result).hasSize(4);
    assertThat(result.get(0).stepType()).isEqualTo(ValidationStepType.STRUCTURAL_A);
    assertThat(result.get(0).enabled()).isTrue();
    assertThat(result.get(1).stepType()).isEqualTo(ValidationStepType.SANDBOX_B);
    assertThat(result.get(2).stepType()).isEqualTo(ValidationStepType.SEMANTIC_C);
    assertThat(result.get(2).enabled()).isFalse();
    assertThat(result.get(3).stepType()).isEqualTo(ValidationStepType.TDR_D);
  }

  @Test
  @DisplayName("findAllOrderedByExecution returns empty list when no configs exist")
  void findAllOrdered_emptyList() {
    when(jpaRepository.findAllByOrderByExecutionOrderAsc()).thenReturn(List.of());
    assertThat(repository.findAllOrderedByExecution()).isEmpty();
  }

  // ── save (upsert) ───────────────────────────────────────────────────────

  @Test
  @DisplayName("save creates new entity when stepType not found")
  void save_newConfig_createsEntity() {
    UUID id = UUID.randomUUID();
    var config =
        new ValidationPipelineConfiguration(
            id, ValidationStepType.TDR_D, true, 4, Map.of("model", "qwen"));

    when(jpaRepository.findByStepType("TDR_D")).thenReturn(Optional.empty());

    var savedEntity = buildEntity("TDR_D", true, 4);
    savedEntity.setId(id);
    savedEntity.setConfigurationPayload(Map.of("model", "qwen"));
    when(jpaRepository.save(any())).thenReturn(savedEntity);

    ValidationPipelineConfiguration result = repository.save(config);
    assertThat(result.stepType()).isEqualTo(ValidationStepType.TDR_D);
    assertThat(result.enabled()).isTrue();
    verify(jpaRepository).save(any());
  }

  @Test
  @DisplayName("save updates existing entity when stepType found")
  void save_existingConfig_updatesEntity() {
    var existing = buildEntity("STRUCTURAL_A", true, 1);
    existing.setConfigurationPayload(Map.of("old", "value"));

    when(jpaRepository.findByStepType("STRUCTURAL_A")).thenReturn(Optional.of(existing));
    when(jpaRepository.save(existing)).thenReturn(existing);

    UUID id = UUID.randomUUID();
    var config =
        new ValidationPipelineConfiguration(
            id, ValidationStepType.STRUCTURAL_A, false, 5, Map.of("new", "value"));

    repository.save(config);

    assertThat(existing.getIsEnabled()).isFalse();
    assertThat(existing.getExecutionOrder()).isEqualTo(5);
    assertThat(existing.getConfigurationPayload()).containsEntry("new", "value");
    verify(jpaRepository).save(existing);
  }

  // ── saveAll ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("saveAll saves multiple configs and returns results")
  void saveAll_savesMultiple() {
    var configA =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.STRUCTURAL_A, true, 1, Map.of());
    var configC =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.SEMANTIC_C, false, 3, Map.of());

    when(jpaRepository.findByStepType(anyString())).thenReturn(Optional.empty());

    var savedA = buildEntity("STRUCTURAL_A", true, 1);
    savedA.setId(configA.id());
    var savedC = buildEntity("SEMANTIC_C", false, 3);
    savedC.setId(configC.id());

    when(jpaRepository.save(any(ValidationPipelineConfigEntity.class)))
        .thenReturn(savedA)
        .thenReturn(savedC);

    List<ValidationPipelineConfiguration> result = repository.saveAll(List.of(configA, configC));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).stepType()).isEqualTo(ValidationStepType.STRUCTURAL_A);
    assertThat(result.get(1).stepType()).isEqualTo(ValidationStepType.SEMANTIC_C);
    verify(jpaRepository, times(2)).save(any());
  }

  @Test
  @DisplayName("saveAll with empty list returns empty result")
  void saveAll_emptyList() {
    assertThat(repository.saveAll(List.of())).isEmpty();
    verify(jpaRepository, never()).save(any());
  }

  // ── Constructor guard ───────────────────────────────────────────────────

  @Test
  @DisplayName("Constructor rejects null repository")
  void constructor_nullRepository_throws() {
    assertThatThrownBy(() -> new JpaValidationPipelineRepository(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ValidationPipelineConfigJpaRepository");
  }

  // ── Helper ──────────────────────────────────────────────────────────────

  private ValidationPipelineConfigEntity buildEntity(String stepType, boolean enabled, int order) {
    var entity = new ValidationPipelineConfigEntity();
    entity.setId(UUID.randomUUID());
    entity.setStepType(stepType);
    entity.setIsEnabled(enabled);
    entity.setExecutionOrder(order);
    entity.setConfigurationPayload(Map.of());
    return entity;
  }
}
