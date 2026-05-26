package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only REST controller exposing CRUD operations on the model catalog. */
@RestController
@RequestMapping("/api/v1/admin/models")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminModelController {

  private final CatalogModelManager catalogModelManager;

  public AdminModelController(CatalogModelManager catalogModelManager) {
    this.catalogModelManager =
        Objects.requireNonNull(catalogModelManager, "CatalogModelManager must not be null");
  }

  /**
   * Adds a new model definition to the catalog.
   *
   * @param dto The CatalogModelDto payload.
   * @return The persisted CatalogModelDto.
   */
  @PostMapping
  public ResponseEntity<CatalogModelDto> createModel(@RequestBody CatalogModelDto dto) {
    return ResponseEntity.ok(catalogModelManager.saveModel(dto));
  }

  /**
   * Updates an existing model definition in the catalog.
   *
   * @param id The ID of the model to update.
   * @param dto The CatalogModelDto payload.
   * @return The updated CatalogModelDto.
   */
  @PutMapping("/{id}")
  public ResponseEntity<CatalogModelDto> updateModel(
      @PathVariable Integer id, @RequestBody CatalogModelDto dto) {
    CatalogModelDto toUpdate =
        new CatalogModelDto(
            id,
            dto.modelName(),
            dto.modelLabel(),
            dto.category(),
            dto.options(),
            dto.isDefault(),
            dto.providerName());
    return ResponseEntity.ok(catalogModelManager.saveModel(toUpdate));
  }

  /**
   * Deletes a model definition by its database ID.
   *
   * @param id The ID of the model to delete.
   * @return Empty 200 OK response.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteModel(@PathVariable Integer id) {
    catalogModelManager.deleteModel(id);
    return ResponseEntity.ok().build();
  }
}
