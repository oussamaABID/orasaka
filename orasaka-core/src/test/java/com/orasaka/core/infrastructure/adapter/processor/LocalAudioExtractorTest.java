package com.orasaka.core.infrastructure.adapter.processor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalAudioExtractorTest {

  @Test
  @DisplayName("extractAudio returns empty array for null input")
  void returnsEmptyForNull() {
    var extractor = new LocalAudioExtractor();
    byte[] result = extractor.extractAudio(null);
    assertNotNull(result);
    assertEquals(0, result.length);
  }

  @Test
  @DisplayName("extractAudio returns empty array for empty input")
  void returnsEmptyForEmpty() {
    var extractor = new LocalAudioExtractor();
    byte[] result = extractor.extractAudio(new byte[0]);
    assertNotNull(result);
    assertEquals(0, result.length);
  }

  @Test
  @DisplayName("extractAudio throws exception for invalid video bytes")
  void throwsForInvalidBytes() {
    var extractor = new LocalAudioExtractor();
    // Since these are random non-video bytes, FFmpeg should fail and the method should throw an
    // exception.
    byte[] dummyBytes = new byte[] {1, 2, 3, 4, 5};
    assertThrows(IllegalStateException.class, () -> extractor.extractAudio(dummyBytes));
  }
}
