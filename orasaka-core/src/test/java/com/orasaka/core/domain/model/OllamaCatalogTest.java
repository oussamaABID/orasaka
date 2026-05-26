package com.orasaka.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class OllamaCatalogTest {

  @Test
  void shouldConstructOllamaCatalogSuccessfully() {
    OllamaModel model = new OllamaModel("name", "modelTag", "digestValue");
    OllamaCatalog catalog = new OllamaCatalog(List.of(model));
    assertThat(catalog.models()).hasSize(1);
    assertThat(catalog.models().get(0)).isEqualTo(model);
  }

  @Test
  void shouldThrowWhenConstructorArgumentsAreNull() {
    assertThatThrownBy(() -> new OllamaCatalog(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("models list cannot be null");
  }
}
