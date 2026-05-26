package com.orasaka.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OllamaModelTest {

  @Test
  void shouldConstructOllamaModelSuccessfully() {
    OllamaModel model = new OllamaModel("name", "modelTag", "digestValue");
    assertThat(model.name()).isEqualTo("name");
    assertThat(model.model()).isEqualTo("modelTag");
    assertThat(model.digest()).isEqualTo("digestValue");
  }

  @Test
  void shouldThrowWhenConstructorArgumentsAreNull() {
    assertThatThrownBy(() -> new OllamaModel(null, "tag", "digest"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("name cannot be null");

    assertThatThrownBy(() -> new OllamaModel("name", null, "digest"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("model cannot be null");

    assertThatThrownBy(() -> new OllamaModel("name", "tag", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("digest cannot be null");
  }
}
