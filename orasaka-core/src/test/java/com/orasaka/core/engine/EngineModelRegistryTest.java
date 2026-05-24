package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.support.OrasakaException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * Unit tests for {@link EngineModelRegistry} covering provider resolution, chat model lookup, and
 * fallback model strategies.
 */
class EngineModelRegistryTest {

  private static CoreProperties props(String defaultProvider) {
    return new CoreProperties(defaultProvider, Map.of(), null, null, null);
  }

  private static CoreProperties propsWithOverrides(
      String defaultProvider, Map<String, CoreProperties.ProviderConfig> overrides) {
    return new CoreProperties(defaultProvider, overrides, null, null, null);
  }

  @Nested
  @DisplayName("getActiveProvider()")
  class ActiveProvider {

    @Test
    @DisplayName("returns configured default provider")
    void returnsDefaultProvider() {
      var registry = createRegistry(props("ollama"));
      assertEquals("ollama", registry.getActiveProvider());
    }

    @Test
    @DisplayName("throws OrasakaException when default provider is null")
    void throwsWhenNull() {
      var registry = createRegistry(props(null));
      assertThrows(OrasakaException.class, registry::getActiveProvider);
    }

    @Test
    @DisplayName("throws OrasakaException when default provider is blank")
    void throwsWhenBlank() {
      var registry = createRegistry(props("  "));
      assertThrows(OrasakaException.class, registry::getActiveProvider);
    }
  }

  @Nested
  @DisplayName("getChatModel()")
  class GetChatModel {

    @Test
    @DisplayName("returns registered chat model for provider")
    void returnsRegisteredModel() {
      ChatModel mockModel = mock(ChatModel.class);
      var registry = createRegistryWith(Map.of("ollama", mockModel), props("ollama"));
      assertSame(mockModel, registry.getChatModel("ollama"));
    }

    @Test
    @DisplayName("throws OrasakaException for unregistered provider")
    void throwsForUnregistered() {
      var registry = createRegistry(props("ollama"));
      assertThrows(OrasakaException.class, () -> registry.getChatModel("unknown"));
    }
  }

  @Nested
  @DisplayName("getActiveEmbeddingModel()")
  class GetEmbeddingModel {

    @Test
    @DisplayName("returns registered embedding model")
    void returnsEmbedding() {
      EmbeddingModel mockEmbed = mock(EmbeddingModel.class);
      var registry =
          new EngineModelRegistry(
              Map.of(), Map.of(), Map.of("ollama", mockEmbed), Map.of(), props("ollama"));
      assertSame(mockEmbed, registry.getActiveEmbeddingModel());
    }

    @Test
    @DisplayName("throws OrasakaException when embedding model not found")
    void throwsWhenMissing() {
      var registry = createRegistry(props("ollama"));
      assertThrows(OrasakaException.class, registry::getActiveEmbeddingModel);
    }
  }

  @Nested
  @DisplayName("getBaseUrl()")
  class GetBaseUrl {

    @Test
    @DisplayName("returns base URL from provider overrides")
    void returnsBaseUrl() {
      var overrides =
          Map.of(
              "ollama",
              new CoreProperties.ProviderConfig(
                  "llama3", "http://localhost:11434", null, null, null));
      var registry = createRegistry(propsWithOverrides("ollama", overrides));
      assertEquals("http://localhost:11434", registry.getBaseUrl());
    }

    @Test
    @DisplayName("throws OrasakaException when base URL is missing")
    void throwsWhenMissing() {
      var registry = createRegistry(props("ollama"));
      assertThrows(OrasakaException.class, registry::getBaseUrl);
    }
  }

  @Nested
  @DisplayName("getActiveImageModel() fallback chain")
  class ImageModelFallback {

    @Test
    @DisplayName("returns fallback placeholder model when no models registered")
    void returnsFallbackModel() {
      var registry = createRegistry(props("ollama"));
      assertNotNull(registry.getActiveImageModel());
    }
  }

  @Nested
  @DisplayName("getActiveSpeechModel() fallback chain")
  class SpeechModelFallback {

    @Test
    @DisplayName("returns fallback no-op model when no models registered")
    void returnsFallbackModel() {
      var registry = createRegistry(props("ollama"));
      assertNotNull(registry.getActiveSpeechModel());
    }
  }

  // ── Factory helpers ──────────────────────────────────────────────────
  private static EngineModelRegistry createRegistry(CoreProperties props) {
    return new EngineModelRegistry(Map.of(), Map.of(), Map.of(), Map.of(), props);
  }

  private static EngineModelRegistry createRegistryWith(
      Map<String, ChatModel> chatModels, CoreProperties props) {
    return new EngineModelRegistry(chatModels, Map.of(), Map.of(), Map.of(), props);
  }
}
