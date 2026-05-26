package com.orasaka.interceptor.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

class ValidationAutoConfigurationTest {

  @Test
  @DisplayName(
      "ValidationAutoConfiguration registers CostShieldInterceptor and QuantumValidationAdvisor")
  void registersBeans() {
    ValidationAutoConfiguration config = new ValidationAutoConfiguration();

    CostShieldInterceptor costShield = config.costShieldInterceptor();
    assertThat(costShield).isNotNull();

    QuantumValidationProperties props =
        new QuantumValidationProperties(true, 3, true, true, true, false);
    ChatModel chatModel = mock(ChatModel.class);
    Map<String, ChatModel> chatModels = Map.of("openai", chatModel);

    QuantumValidationAdvisor advisorWithModel =
        config.quantumValidationAdvisor(props, chatModels, null, null);
    assertThat(advisorWithModel).isNotNull();

    QuantumValidationAdvisor advisorWithoutModel =
        config.quantumValidationAdvisor(props, Collections.emptyMap(), null, null);
    assertThat(advisorWithoutModel).isNotNull();
  }
}
