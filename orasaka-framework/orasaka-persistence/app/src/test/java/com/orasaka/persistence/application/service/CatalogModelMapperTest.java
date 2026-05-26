package com.orasaka.persistence.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.CatalogModelEntity;
import org.junit.jupiter.api.Test;

class CatalogModelMapperTest {

  @Test
  void toDto_mapsAllFields() {
    var entity = new CatalogModelEntity();
    entity.setId(1);
    entity.setModelName("gpt-4");
    entity.setModelLabel("GPT-4");
    entity.setCategory("chat");
    entity.setOptions("{}");
    entity.setIsDefault(true);
    entity.setProviderName("openai");
    entity.setMaxSteps(100);
    entity.setRecommendedFps(24);
    entity.setSupportedHardware("GPU");
    CatalogModelDto dto = CatalogModelMapper.toDto(entity);
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
  void toDto_null_returnsNull() {
    assertNull(CatalogModelMapper.toDto(null));
  }

  @Test
  void toEntity_mapsAllFields() {
    var dto =
        new CatalogModelDto(1, "gpt-4", "GPT-4", "chat", "{}", true, "openai", 100, 24, "GPU");
    CatalogModelEntity entity = CatalogModelMapper.toEntity(dto);
    assertEquals(1, entity.getId());
    assertEquals("gpt-4", entity.getModelName());
    assertEquals("GPT-4", entity.getModelLabel());
    assertEquals("chat", entity.getCategory());
    assertEquals("{}", entity.getOptions());
    assertTrue(entity.getIsDefault());
    assertEquals("openai", entity.getProviderName());
    assertEquals(100, entity.getMaxSteps());
    assertEquals(24, entity.getRecommendedFps());
    assertEquals("GPU", entity.getSupportedHardware());
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(CatalogModelMapper.toEntity(null));
  }
}
