package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.ValidationPipelineConfiguration;
import com.orasaka.core.domain.model.ValidationStepType;
import com.orasaka.core.domain.ports.outbound.ValidationPipelineRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Unit tests for {@link ValidationAdminController}. */
class ValidationAdminControllerTest {

  private final ValidationPipelineRepository repository = mock(ValidationPipelineRepository.class);
  private final ValidationAdminController controller = new ValidationAdminController(repository);

  // ── GET /api/v1/admin/validation-pipeline ────────────────────────────────

  @Test
  @DisplayName("GET returns ordered validation pipeline configs")
  void getValidationPipeline_returnsOrderedConfigs() {
    var configA =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.STRUCTURAL_A, true, 1, Map.of());
    var configD =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.TDR_D, false, 4, Map.of());

    when(repository.findAllOrderedByExecution()).thenReturn(List.of(configA, configD));

    ResponseEntity<List<ValidationPipelineConfiguration>> response =
        controller.getValidationPipeline();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
    assertThat(response.getBody().get(0).stepType()).isEqualTo(ValidationStepType.STRUCTURAL_A);
    assertThat(response.getBody().get(1).stepType()).isEqualTo(ValidationStepType.TDR_D);
  }

  @Test
  @DisplayName("GET returns empty list when no configs exist")
  void getValidationPipeline_emptyList() {
    when(repository.findAllOrderedByExecution()).thenReturn(List.of());

    ResponseEntity<List<ValidationPipelineConfiguration>> response =
        controller.getValidationPipeline();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  // ── PUT /api/v1/admin/validation-pipeline ────────────────────────────────

  @Test
  @DisplayName("PUT saves configs and returns saved results")
  void updateValidationPipeline_savesAndReturns() {
    var inputA =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.STRUCTURAL_A, false, 2, Map.of());
    var inputD =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.TDR_D, true, 1, Map.of("model", "qwen"));

    var savedA =
        new ValidationPipelineConfiguration(
            inputA.id(), ValidationStepType.STRUCTURAL_A, false, 2, Map.of());
    var savedD =
        new ValidationPipelineConfiguration(
            inputD.id(), ValidationStepType.TDR_D, true, 1, Map.of("model", "qwen"));

    when(repository.saveAll(List.of(inputA, inputD))).thenReturn(List.of(savedA, savedD));

    ResponseEntity<List<ValidationPipelineConfiguration>> response =
        controller.updateValidationPipeline(List.of(inputA, inputD));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
    verify(repository).saveAll(List.of(inputA, inputD));
  }

  @Test
  @DisplayName("PUT with empty list saves nothing")
  void updateValidationPipeline_emptyInput() {
    when(repository.saveAll(List.of())).thenReturn(List.of());

    ResponseEntity<List<ValidationPipelineConfiguration>> response =
        controller.updateValidationPipeline(List.of());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  @DisplayName("PUT with all tiers reordered")
  void updateValidationPipeline_reorderAllTiers() {
    // Reorder: D→1, C→2, B→3, A→4 (inverted)
    var configD =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.TDR_D, true, 1, Map.of());
    var configC =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.SEMANTIC_C, true, 2, Map.of());
    var configB =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.SANDBOX_B, true, 3, Map.of());
    var configA =
        new ValidationPipelineConfiguration(
            UUID.randomUUID(), ValidationStepType.STRUCTURAL_A, true, 4, Map.of());

    List<ValidationPipelineConfiguration> input = List.of(configD, configC, configB, configA);
    when(repository.saveAll(input)).thenReturn(input);

    ResponseEntity<List<ValidationPipelineConfiguration>> response =
        controller.updateValidationPipeline(input);

    assertThat(response.getBody().get(0).executionOrder()).isEqualTo(1);
    assertThat(response.getBody().get(0).stepType()).isEqualTo(ValidationStepType.TDR_D);
    assertThat(response.getBody().get(3).executionOrder()).isEqualTo(4);
  }

  // ── Constructor guard ───────────────────────────────────────────────────

  @Test
  @DisplayName("Constructor rejects null repository")
  void constructor_nullRepository_throws() {
    assertThatThrownBy(() -> new ValidationAdminController(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ValidationPipelineRepository");
  }
}
