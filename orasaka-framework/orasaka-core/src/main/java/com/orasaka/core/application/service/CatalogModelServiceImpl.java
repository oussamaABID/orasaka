package com.orasaka.core.application.service;

import com.orasaka.core.domain.model.model.CatalogModelInfo;
import com.orasaka.core.domain.ports.inbound.CatalogModelService;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Package-private implementation of the CatalogModelService inbound port. Delegates to the
 * out-of-boundary persistence package. Follows ERR-105 (Interface-Driven Boundaries).
 */
@Service
class CatalogModelServiceImpl implements CatalogModelService {

  private final CatalogModelManager catalogModelManager;

  CatalogModelServiceImpl(CatalogModelManager catalogModelManager) {
    this.catalogModelManager =
        Objects.requireNonNull(catalogModelManager, "CatalogModelManager cannot be null");
  }

  @Override
  public List<CatalogModelInfo> getAllModels() {
    return catalogModelManager.getAllModels().stream().map(CatalogModelInfoMapper::toInfo).toList();
  }

  @Override
  public List<CatalogModelInfo> getModelsByCategory(String category) {
    return catalogModelManager.getModelsByCategory(category).stream()
        .map(CatalogModelInfoMapper::toInfo)
        .toList();
  }

  @Override
  public Optional<CatalogModelInfo> getDefaultModelByCategory(String category) {
    return catalogModelManager
        .getDefaultModelByCategory(category)
        .map(CatalogModelInfoMapper::toInfo);
  }

  @Override
  public CatalogModelInfo saveModel(CatalogModelInfo dto) {
    return CatalogModelInfoMapper.toInfo(
        catalogModelManager.saveModel(CatalogModelInfoMapper.toDto(dto)));
  }

  @Override
  public void deleteModel(Integer id) {
    catalogModelManager.deleteModel(id);
  }
}
