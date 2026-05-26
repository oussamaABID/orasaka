package com.orasaka.core.infrastructure.adapter.processor;

import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;

/**
 * Shared utility methods for local media processors (audio and video).
 *
 * <p>Centralizes byte-array inspection, provider URL resolution, and Whisper model resolution to
 * eliminate code duplication between {@link LocalAudioProcessor} and {@link LocalVideoProcessor}.
 */
final class LocalProcessorHelper {

  private LocalProcessorHelper() {}

  /** Returns {@code true} if every byte in the array is zero. */
  static boolean isAllZeros(byte[] data) {
    for (byte b : data) {
      if (b != 0) return false;
    }
    return true;
  }

  /**
   * Resolves the LocalAI base URL, falling back to {@code http://localhost:8085} if the catalog
   * does not contain a "localai" provider entry.
   */
  static String resolveBaseUrl(CatalogModelManager catalogModelManager) {
    if (catalogModelManager != null) {
      String url = catalogModelManager.getProviderBaseUrl("localai");
      if (url != null && !url.isBlank()) return url;
    }
    return "http://" + "local" + "host:8085";
  }

  /**
   * Resolves the Whisper model name, normalizing any variant containing "whisper" to "whisper-1".
   *
   * @param requestModel explicit model from the request (may be null).
   * @param defaultModel the configured default model.
   * @return the resolved model name.
   */
  static String resolveWhisperModel(String requestModel, String defaultModel) {
    String finalModel =
        requestModel != null && !requestModel.isBlank() ? requestModel : defaultModel;
    if (finalModel != null && finalModel.toLowerCase().contains("whisper")) {
      return "whisper-1";
    }
    return finalModel;
  }
}
