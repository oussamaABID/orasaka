package com.orasaka.core.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.model.CatalogModelInfo;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import org.junit.jupiter.api.Test;

class CatalogModelInfoMapperTest {

  @Test
  void toInfo_null_returnsNull() {
    assertNull(CatalogModelInfoMapper.toInfo(null));
  }

  @Test
  void toInfo_validDto_mapsAllFields() {
    var dto =
        new CatalogModelDto(
            1, "model-name", "Model Label", "chat", "{}", true, "openai", 100, 24, "GPU");

    CatalogModelInfo result = CatalogModelInfoMapper.toInfo(dto);

    assertNotNull(result);
    assertEquals(1, result.id());
    assertEquals("model-name", result.modelName());
    assertEquals("Model Label", result.modelLabel());
    assertEquals("chat", result.category());
    assertEquals("{}", result.options());
    assertTrue(result.isDefault());
    assertEquals("openai", result.providerName());
    assertEquals(100, result.maxSteps());
    assertEquals(24, result.recommendedFps());
    assertEquals("GPU", result.supportedHardware());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(CatalogModelInfoMapper.toDto(null));
  }

  @Test
  void toDto_validInfo_mapsAllFields() {
    var info =
        new CatalogModelInfo(
            1, "model-name", "Model Label", "chat", "{}", true, "openai", 100, 24, "GPU");

    CatalogModelDto result = CatalogModelInfoMapper.toDto(info);

    assertNotNull(result);
    assertEquals(1, result.id());
    assertEquals("model-name", result.modelName());
    assertEquals("Model Label", result.modelLabel());
    assertEquals("chat", result.category());
    assertTrue(result.isDefault());
    assertEquals("openai", result.providerName());
  }

  @Test
  void roundTrip_dtoToInfoAndBack_preservesData() {
    var original =
        new CatalogModelDto(
            2, "model", "label", "video", "options-json", false, "ollama", 50, 30, "MPS");

    CatalogModelInfo info = CatalogModelInfoMapper.toInfo(original);
    CatalogModelDto roundTripped = CatalogModelInfoMapper.toDto(info);

    assertEquals(original, roundTripped);
  }
}
