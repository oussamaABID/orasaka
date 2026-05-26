package com.orasaka.core.application.service;

import com.orasaka.core.domain.model.model.CatalogModelInfo;
import com.orasaka.persistence.domain.model.CatalogModelDto;

/**
 * Package-private final mapper mapping CatalogModelDto to CatalogModelInfo and vice-versa. Follows
 * ERR-107 (Mapper Isolation Invariant).
 */
final class CatalogModelInfoMapper {

  private CatalogModelInfoMapper() {}

  static CatalogModelInfo toInfo(CatalogModelDto dto) {
    if (dto == null) {
      return null;
    }
    return new CatalogModelInfo(
        dto.id(),
        dto.modelName(),
        dto.modelLabel(),
        dto.category(),
        dto.options(),
        dto.isDefault(),
        dto.providerName(),
        dto.maxSteps(),
        dto.recommendedFps(),
        dto.supportedHardware());
  }

  static CatalogModelDto toDto(CatalogModelInfo info) {
    if (info == null) {
      return null;
    }
    return new CatalogModelDto(
        info.id(),
        info.modelName(),
        info.modelLabel(),
        info.category(),
        info.options(),
        info.isDefault(),
        info.providerName(),
        info.maxSteps(),
        info.recommendedFps(),
        info.supportedHardware());
  }
}
