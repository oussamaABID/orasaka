package com.orasaka.gateway.application.service;

import static org.assertj.core.api.Assertions.*;

import com.orasaka.core.domain.model.ValidationPipelineConfiguration;
import com.orasaka.core.domain.model.ValidationStepType;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.ValidationPipelineConfigEntity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ValidationPipelineConfigMapper}. */
class ValidationPipelineConfigMapperTest {

  @Test
  @DisplayName("toRecord maps all fields correctly including String → enum conversion")
  void toRecord_mapsAllFields() {
    UUID id = UUID.randomUUID();
    var entity = new ValidationPipelineConfigEntity();
    entity.setId(id);
    entity.setStepType("STRUCTURAL_A");
    entity.setIsEnabled(true);
    entity.setExecutionOrder(1);
    entity.setConfigurationPayload(Map.of("schemaStrict", true));

    ValidationPipelineConfiguration config = ValidationPipelineConfigMapper.toRecord(entity);

    assertThat(config.id()).isEqualTo(id);
    assertThat(config.stepType()).isEqualTo(ValidationStepType.STRUCTURAL_A);
    assertThat(config.enabled()).isTrue();
    assertThat(config.executionOrder()).isEqualTo(1);
    assertThat(config.configurationPayload()).containsEntry("schemaStrict", true);
  }

  @Test
  @DisplayName("toRecord handles null payload gracefully")
  void toRecord_nullPayloadHandled() {
    var entity = new ValidationPipelineConfigEntity();
    entity.setId(UUID.randomUUID());
    entity.setStepType("SANDBOX_B");
    entity.setIsEnabled(false);
    entity.setExecutionOrder(2);
    entity.setConfigurationPayload(null);

    ValidationPipelineConfiguration config = ValidationPipelineConfigMapper.toRecord(entity);
    assertThat(config.configurationPayload()).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("toRecord maps null isEnabled to false")
  void toRecord_nullIsEnabledMapsFalse() {
    var entity = new ValidationPipelineConfigEntity();
    entity.setId(UUID.randomUUID());
    entity.setStepType("TDR_D");
    entity.setIsEnabled(null);
    entity.setExecutionOrder(4);
    entity.setConfigurationPayload(Map.of());

    ValidationPipelineConfiguration config = ValidationPipelineConfigMapper.toRecord(entity);
    assertThat(config.enabled()).isFalse();
  }

  @Test
  @DisplayName("toEntity maps all fields correctly including enum → String conversion")
  void toEntity_mapsAllFields() {
    UUID id = UUID.randomUUID();
    var config =
        new ValidationPipelineConfiguration(
            id, ValidationStepType.SEMANTIC_C, true, 3, Map.of("debateTemperature", 0.0));

    ValidationPipelineConfigEntity entity = ValidationPipelineConfigMapper.toEntity(config);

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getStepType()).isEqualTo("SEMANTIC_C");
    assertThat(entity.getIsEnabled()).isTrue();
    assertThat(entity.getExecutionOrder()).isEqualTo(3);
    assertThat(entity.getConfigurationPayload()).containsEntry("debateTemperature", 0.0);
  }

  @Test
  @DisplayName("Round-trip: record → entity → record preserves data")
  void roundTrip_preservesData() {
    UUID id = UUID.randomUUID();
    var original =
        new ValidationPipelineConfiguration(
            id, ValidationStepType.TDR_D, false, 4, Map.of("modelName", "qwen2.5-coder:7b"));

    ValidationPipelineConfigEntity entity = ValidationPipelineConfigMapper.toEntity(original);
    ValidationPipelineConfiguration roundTripped = ValidationPipelineConfigMapper.toRecord(entity);

    assertThat(roundTripped).isEqualTo(original);
  }

  @Test
  @DisplayName("Round-trip for each ValidationStepType")
  void roundTrip_eachStepType() {
    for (ValidationStepType type : ValidationStepType.values()) {
      UUID id = UUID.randomUUID();
      var config =
          new ValidationPipelineConfiguration(id, type, true, type.defaultOrder(), Map.of());

      var entity = ValidationPipelineConfigMapper.toEntity(config);
      assertThat(entity.getStepType()).isEqualTo(type.name());

      var result = ValidationPipelineConfigMapper.toRecord(entity);
      assertThat(result.stepType()).isEqualTo(type);
      assertThat(result.executionOrder()).isEqualTo(type.defaultOrder());
    }
  }

  @Test
  @DisplayName("toRecord with invalid step type string throws IllegalArgumentException")
  void toRecord_invalidStepTypeThrows() {
    var entity = new ValidationPipelineConfigEntity();
    entity.setId(UUID.randomUUID());
    entity.setStepType("INVALID_TIER");
    entity.setIsEnabled(true);
    entity.setExecutionOrder(99);
    entity.setConfigurationPayload(Map.of());

    assertThatThrownBy(() -> ValidationPipelineConfigMapper.toRecord(entity))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
