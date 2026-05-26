package com.orasaka.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RoutingMode")
class RoutingModeTest {

  @Test
  @DisplayName("DETERMINISTIC has correct description")
  void deterministicHasCorrectDescription() {
    assertThat(RoutingMode.DETERMINISTIC.description())
        .isEqualTo("Database-driven deterministic ordering");
  }

  @Test
  @DisplayName("AGENTIC has correct description")
  void agenticHasCorrectDescription() {
    assertThat(RoutingMode.AGENTIC.description())
        .isEqualTo("LLM-driven runtime sequence generation");
  }

  @Test
  @DisplayName("valueOf resolves DETERMINISTIC")
  void valueOfResolvesDeterministic() {
    assertThat(RoutingMode.valueOf("DETERMINISTIC")).isEqualTo(RoutingMode.DETERMINISTIC);
  }

  @Test
  @DisplayName("valueOf resolves AGENTIC")
  void valueOfResolvesAgentic() {
    assertThat(RoutingMode.valueOf("AGENTIC")).isEqualTo(RoutingMode.AGENTIC);
  }

  @Test
  void canParseSemantic() {
    assertThat(RoutingMode.valueOf("SEMANTIC")).isEqualTo(RoutingMode.SEMANTIC);
  }

  @Test
  @DisplayName("values contains exactly 3 modes")
  void valuesContainsExactlyThreeModes() {
    assertThat(RoutingMode.values()).hasSize(3);
  }
}
