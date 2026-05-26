package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;

/**
 * Shared utility methods for AMQP job execution strategies.
 *
 * <p>Centralizes prompt extraction and model resolution logic to eliminate code duplication across
 * strategy implementations (ERR-107 mapper isolation adapted for utility methods).
 */
final class JobStrategyHelper {

  private JobStrategyHelper() {}

  /**
   * Extracts the prompt from the job payload, checking both "prompt" and "text" keys.
   *
   * @throws IllegalArgumentException if neither key is present.
   */
  static String extractPrompt(JobMessage message) {
    String prompt = (String) message.payload().get("prompt");
    if (prompt == null) {
      prompt = (String) message.payload().get("text");
    }
    if (prompt == null) {
      throw new IllegalArgumentException("Payload does not contain prompt or text field");
    }
    return prompt;
  }

  /**
   * Resolves the AI model name from the job message, falling back to the catalog default for the
   * given category.
   *
   * @param message the incoming job message (may carry an explicit model override).
   * @param category the model category (e.g. "image", "speech", "video").
   * @param fallbackModel the hard-coded default if no catalog entry exists.
   * @param catalogModelManager the catalog service for default model lookup.
   * @return the resolved model name.
   */
  static String resolveModel(
      JobMessage message,
      String category,
      String fallbackModel,
      CatalogModelManager catalogModelManager) {
    String model = message.model();
    if (model != null && !model.isBlank()) {
      return model;
    }
    return catalogModelManager
        .getDefaultModelByCategory(category)
        .map(CatalogModelDto::modelName)
        .orElse(fallbackModel);
  }
}
