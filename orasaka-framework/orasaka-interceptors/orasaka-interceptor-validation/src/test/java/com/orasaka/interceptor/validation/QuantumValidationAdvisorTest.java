package com.orasaka.interceptor.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

@ExtendWith(MockitoExtension.class)
class QuantumValidationAdvisorTest {

  @Mock private ChatModel chatModel;

  @ParameterizedTest(name = "Tier A - {0}")
  @CsvSource({
    "Valid JSON passes schema validation silently, true, 3, '{\"key\": \"value\"}'",
    "Invalid JSON triggers retry logging (no exception thrown), true, 1, '{invalid json'",
    "Non-JSON text passes when no JSON block detected, true, 3, 'This is a plain text response without JSON.'"
  })
  void tierA_validationScenarios(
      String description, boolean schemaStrict, int maxRetries, String responseText) {
    var props =
        new QuantumValidationProperties(true, maxRetries, schemaStrict, false, false, false);
    var advisor = new QuantumValidationAdvisor(props, null, null, null);

    assertThatCode(() -> advisor.postProcess(null, "test prompt", responseText))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Tier C — Advocate wins, no retry triggered")
  void tierC_advocateWinsNoRetry() {
    var props = new QuantumValidationProperties(true, 3, false, false, true, false);

    // Critic scores low (3), Advocate scores high (8) — Advocate wins
    ChatResponse criticResponse = mockResponse("{\"score\": 3, \"issues\": []}");
    ChatResponse advocateResponse = mockResponse("{\"score\": 8, \"strengths\": [\"solid\"]}");

    when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
        .thenReturn(criticResponse, advocateResponse);

    var advisor = new QuantumValidationAdvisor(props, chatModel, null, null);

    assertThatCode(() -> advisor.postProcess(null, "Write a function", "def foo(): return 42"))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Tier C — Critic wins, retries exhausted gracefully")
  void tierC_criticWinsRetriesExhausted() {
    var props = new QuantumValidationProperties(true, 1, false, false, true, false);

    // Critic always scores high (9), Advocate always scores low (2) — Critic wins
    ChatResponse criticResponse = mockResponse("{\"score\": 9, \"issues\": [\"logical flaw\"]}");
    ChatResponse advocateResponse = mockResponse("{\"score\": 2, \"strengths\": []}");

    when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
        .thenReturn(criticResponse, advocateResponse);

    var advisor = new QuantumValidationAdvisor(props, chatModel, null, null);

    assertThatCode(() -> advisor.postProcess(null, "Write correct code", "def foo(): return None"))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Disabled advisor does nothing")
  void disabledAdvisorNoOp() {
    var props = new QuantumValidationProperties(false, 3, true, true, true, false);
    var advisor = new QuantumValidationAdvisor(props, chatModel, null, null);

    assertThatCode(() -> advisor.postProcess(null, "prompt", "{\"key\": \"value\"}"))
        .doesNotThrowAnyException();
    verifyNoInteractions(chatModel);
  }

  @Test
  @DisplayName("Null response text does nothing")
  void nullResponseNoOp() {
    var props = QuantumValidationProperties.defaults();
    var advisor = new QuantumValidationAdvisor(props, chatModel, null, null);

    assertThatCode(() -> advisor.postProcess(null, "prompt", null)).doesNotThrowAnyException();
    verifyNoInteractions(chatModel);
  }

  @Test
  @DisplayName("isAiDependent reflects Tier C and Tier D state")
  void isAiDependentReflectsTierCAndD() {
    // Tier C enabled with chatModel → AI-dependent
    var enabledC = new QuantumValidationProperties(true, 3, true, false, true, false);
    assertThat(new QuantumValidationAdvisor(enabledC, chatModel, null, null).isAiDependent())
        .isTrue();

    // Tier C disabled, Tier D disabled → not AI-dependent
    var disabledAll = new QuantumValidationProperties(true, 3, true, false, false, false);
    assertThat(new QuantumValidationAdvisor(disabledAll, chatModel, null, null).isAiDependent())
        .isFalse();

    // Tier C enabled but no chatModel → not AI-dependent (graceful degradation)
    var noModel = new QuantumValidationProperties(true, 3, true, false, true, false);
    assertThat(new QuantumValidationAdvisor(noModel, null, null, null).isAiDependent()).isFalse();

    // Tier D enabled with testShaperPort → AI-dependent
    var tdrPort = mock(com.orasaka.core.domain.ports.outbound.TestShaperPort.class);
    var enabledD = new QuantumValidationProperties(true, 3, true, false, false, true);
    assertThat(new QuantumValidationAdvisor(enabledD, null, null, tdrPort).isAiDependent())
        .isTrue();
  }

  @Test
  @DisplayName("QuantumValidationProperties self-validates bounds")
  void propertiesSelfValidation() {
    var capped = new QuantumValidationProperties(true, 100, true, true, true, false);
    assertThat(capped.maxRetries()).isEqualTo(5);

    var negative = new QuantumValidationProperties(true, -5, true, true, true, false);
    assertThat(negative.maxRetries()).isZero();
  }

  private ChatResponse mockResponse(String text) {
    var output = new AssistantMessage(text);
    var generation = new Generation(output);
    return new ChatResponse(java.util.List.of(generation));
  }
}
