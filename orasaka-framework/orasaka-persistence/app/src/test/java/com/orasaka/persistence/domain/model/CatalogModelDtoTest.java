package com.orasaka.persistence.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CatalogModelDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    var dto =
        new CatalogModelDto(1, "gpt-4", "GPT-4", "chat", "{}", true, "openai", 100, 24, "GPU");
    assertEquals(1, dto.id());
    assertEquals("gpt-4", dto.modelName());
    assertEquals("GPT-4", dto.modelLabel());
    assertEquals("chat", dto.category());
    assertEquals("{}", dto.options());
    assertTrue(dto.isDefault());
    assertEquals("openai", dto.providerName());
    assertEquals(100, dto.maxSteps());
    assertEquals(24, dto.recommendedFps());
    assertEquals("GPU", dto.supportedHardware());
  }

  @Test
  void nullModelName_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new CatalogModelDto(1, null, "label", "chat", "{}", true, "prov", 10, 24, "GPU"));
  }

  @Test
  void blankModelName_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CatalogModelDto(1, "  ", "label", "chat", "{}", true, "prov", 10, 24, "GPU"));
  }

  @Test
  void nullModelLabel_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new CatalogModelDto(1, "model", null, "chat", "{}", true, "prov", 10, 24, "GPU"));
  }

  @Test
  void blankModelLabel_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CatalogModelDto(1, "model", "  ", "chat", "{}", true, "prov", 10, 24, "GPU"));
  }

  @Test
  void equalsAndHashCode() {
    var a = new CatalogModelDto(1, "m", "l", "c", "{}", true, "p", 10, 24, "G");
    var b = new CatalogModelDto(1, "m", "l", "c", "{}", true, "p", 10, 24, "G");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}
