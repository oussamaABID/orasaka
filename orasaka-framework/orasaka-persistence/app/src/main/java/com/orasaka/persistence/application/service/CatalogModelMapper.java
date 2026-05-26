package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.CatalogModelEntity;

/** Package-private final mapper mapping CatalogModelEntity to CatalogModelDto per ERR-107. */
final class CatalogModelMapper {

  private CatalogModelMapper() {
    // Utility class private constructor
  }

  static CatalogModelDto toDto(CatalogModelEntity entity) {
    if (entity == null) {
      return null;
    }
    return new CatalogModelDto(
        entity.getId(),
        entity.getModelName(),
        entity.getModelLabel(),
        entity.getCategory(),
        entity.getOptions(),
        entity.getIsDefault(),
        entity.getProviderName(),
        entity.getMaxSteps(),
        entity.getRecommendedFps(),
        entity.getSupportedHardware());
  }

  static CatalogModelEntity toEntity(CatalogModelDto dto) {
    if (dto == null) {
      return null;
    }
    CatalogModelEntity entity = new CatalogModelEntity();
    entity.setId(dto.id());
    entity.setModelName(dto.modelName());
    entity.setModelLabel(dto.modelLabel());
    entity.setCategory(dto.category());
    entity.setOptions(dto.options());
    entity.setIsDefault(dto.isDefault());
    entity.setProviderName(dto.providerName());
    entity.setMaxSteps(dto.maxSteps());
    entity.setRecommendedFps(dto.recommendedFps());
    entity.setSupportedHardware(dto.supportedHardware());
    return entity;
  }
}
