package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.model.OllamaCatalog;
import com.orasaka.core.domain.ports.outbound.ModelCatalogProvider;
import com.orasaka.gateway.infrastructure.config.ModelCatalogProperties;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller that exposes the discovered and cataloged models on the local node. */
@RestController
@RequestMapping("/api/v1")
public class ModelController {

  private final ModelCatalogProvider modelCatalogProvider;
  private final ModelCatalogProperties modelCatalogProperties;
  private final CatalogModelManager catalogModelManager;

  public ModelController(
      ModelCatalogProvider modelCatalogProvider,
      ModelCatalogProperties modelCatalogProperties,
      CatalogModelManager catalogModelManager) {
    this.modelCatalogProvider =
        Objects.requireNonNull(modelCatalogProvider, "ModelCatalogProvider must not be null");
    this.modelCatalogProperties =
        Objects.requireNonNull(modelCatalogProperties, "ModelCatalogProperties must not be null");
    this.catalogModelManager =
        Objects.requireNonNull(catalogModelManager, "CatalogModelManager must not be null");
  }

  /**
   * Retrieves the local catalog of Ollama models.
   *
   * @return JSON response containing the catalog, or 404 if the registry is offline.
   */
  @GetMapping("/models")
  public ResponseEntity<OllamaCatalog> getModels() {
    return modelCatalogProvider
        .getCatalog()
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Retrieves the list of supported model names grouped by media type.
   *
   * @return JSON response containing the supported models catalog.
   */
  @GetMapping("/models/supported")
  public ResponseEntity<Map<String, List<String>>> getSupportedModels() {
    return ResponseEntity.ok(
        modelCatalogProperties.getModels() != null ? modelCatalogProperties.getModels() : Map.of());
  }

  /**
   * Retrieves the complete AI model catalog definitions from the database.
   *
   * @return JSON response containing all model definitions.
   */
  @GetMapping("/models/catalog")
  public ResponseEntity<List<CatalogModelDto>> getCatalogModels() {
    return ResponseEntity.ok(catalogModelManager.getAllModels());
  }

  /**
   * Retrieves all available AI provider names from the database.
   *
   * @return JSON response containing all provider names.
   */
  @GetMapping("/models/providers")
  public ResponseEntity<List<String>> getProviders() {
    return ResponseEntity.ok(catalogModelManager.getAllProviders());
  }
}
