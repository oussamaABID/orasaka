package com.orasaka.core.infrastructure.adapter.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LocalProcessorHelper}. */
class LocalProcessorHelperTest {

  // --- isAllZeros ---

  @Test
  void isAllZeros_allZeros_returnsTrue() {
    assertTrue(LocalProcessorHelper.isAllZeros(new byte[] {0, 0, 0, 0}));
  }

  @Test
  void isAllZeros_hasNonZero_returnsFalse() {
    assertFalse(LocalProcessorHelper.isAllZeros(new byte[] {0, 1, 0}));
  }

  @Test
  void isAllZeros_emptyArray_returnsTrue() {
    assertTrue(LocalProcessorHelper.isAllZeros(new byte[0]));
  }

  @Test
  void isAllZeros_singleZero_returnsTrue() {
    assertTrue(LocalProcessorHelper.isAllZeros(new byte[] {0}));
  }

  @Test
  void isAllZeros_singleNonZero_returnsFalse() {
    assertFalse(LocalProcessorHelper.isAllZeros(new byte[] {42}));
  }

  // --- resolveBaseUrl ---

  @Test
  void resolveBaseUrl_catalogHasUrl_returnsIt() {
    CatalogModelManager manager = mock(CatalogModelManager.class);
    when(manager.getProviderBaseUrl("localai")).thenReturn("http://my-localai:9090");

    assertEquals("http://my-localai:9090", LocalProcessorHelper.resolveBaseUrl(manager));
  }

  @Test
  void resolveBaseUrl_catalogReturnsNull_fallsBack() {
    CatalogModelManager manager = mock(CatalogModelManager.class);
    when(manager.getProviderBaseUrl("localai")).thenReturn(null);

    String result = LocalProcessorHelper.resolveBaseUrl(manager);
    assertTrue(result.contains("8085"));
  }

  @Test
  void resolveBaseUrl_catalogReturnsBlank_fallsBack() {
    CatalogModelManager manager = mock(CatalogModelManager.class);
    when(manager.getProviderBaseUrl("localai")).thenReturn("  ");

    String result = LocalProcessorHelper.resolveBaseUrl(manager);
    assertTrue(result.contains("8085"));
  }

  @Test
  void resolveBaseUrl_nullManager_fallsBack() {
    String result = LocalProcessorHelper.resolveBaseUrl(null);
    assertTrue(result.contains("8085"));
  }

  // --- resolveWhisperModel ---

  @Test
  void resolveWhisperModel_explicitModel_usesIt() {
    assertEquals(
        "custom-model", LocalProcessorHelper.resolveWhisperModel("custom-model", "whisper-1"));
  }

  @Test
  void resolveWhisperModel_nullModel_usesDefault() {
    assertEquals("whisper-1", LocalProcessorHelper.resolveWhisperModel(null, "whisper-1"));
  }

  @Test
  void resolveWhisperModel_blankModel_usesDefault() {
    assertEquals("whisper-1", LocalProcessorHelper.resolveWhisperModel("  ", "whisper-1"));
  }

  @Test
  void resolveWhisperModel_containsWhisper_normalizesToWhisper1() {
    assertEquals(
        "whisper-1", LocalProcessorHelper.resolveWhisperModel("Whisper-Large-v3", "fallback"));
  }

  @Test
  void resolveWhisperModel_defaultContainsWhisper_normalizesToWhisper1() {
    assertEquals("whisper-1", LocalProcessorHelper.resolveWhisperModel(null, "whisper-large"));
  }

  @Test
  void resolveWhisperModel_nonWhisperDefault_usesDefault() {
    assertEquals("custom-stt", LocalProcessorHelper.resolveWhisperModel(null, "custom-stt"));
  }
}
