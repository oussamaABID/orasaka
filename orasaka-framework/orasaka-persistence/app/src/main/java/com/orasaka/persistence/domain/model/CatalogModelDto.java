package com.orasaka.persistence.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable DTO representing a model definition from the catalog. Enforces compact constructor
 * validation per ERR-106.
 */
public record CatalogModelDto(
    Integer id,
    String modelName,
    String modelLabel,
    String category,
    String options,
    Boolean isDefault,
    String providerName,
    Integer maxSteps,
    Integer recommendedFps,
    String supportedHardware)
    implements Serializable {

  public CatalogModelDto {
    Objects.requireNonNull(modelName, "modelName cannot be null");
    if (modelName.isBlank()) {
      throw new IllegalArgumentException("modelName cannot be blank");
    }
    Objects.requireNonNull(modelLabel, "modelLabel cannot be null");
    if (modelLabel.isBlank()) {
      throw new IllegalArgumentException("modelLabel cannot be blank");
    }
    Objects.requireNonNull(category, "category cannot be null");
    if (category.isBlank()) {
      throw new IllegalArgumentException("category cannot be blank");
    }
    if (isDefault == null) {
      isDefault = Boolean.FALSE;
    }
    if (providerName == null || providerName.isBlank()) {
      providerName = "ollama";
    }
  }

  /** Backwards compatible constructor with 6 parameters. */
  public CatalogModelDto(
      Integer id,
      String modelName,
      String modelLabel,
      String category,
      String options,
      Boolean isDefault) {
    this(id, modelName, modelLabel, category, options, isDefault, "ollama", null, null, null);
  }

  /** Backwards compatible constructor with 7 parameters. */
  public CatalogModelDto(
      Integer id,
      String modelName,
      String modelLabel,
      String category,
      String options,
      Boolean isDefault,
      String providerName) {
    this(id, modelName, modelLabel, category, options, isDefault, providerName, null, null, null);
  }
}
