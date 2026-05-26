package com.orasaka.core.infrastructure.adapter.processor;

import static com.orasaka.test.TestConstants.PROVIDER_OLLAMA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.application.processing.ProcessedAudioPayload;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalAudioProcessorTest {

  private static CoreProperties propsWithAudio() {
    return new CoreProperties(
        PROVIDER_OLLAMA,
        null,
        null,
        null,
        null,
        null,
        null,
        new CoreProperties.AudioConfig(PROVIDER_OLLAMA, "llama3.1:8b", "whisper-1"));
  }

  private static CoreProperties propsWithNullAudio() {
    return new CoreProperties(PROVIDER_OLLAMA, null, null, null, null, null, null, null);
  }

  @Test
  @DisplayName("Constructor throws NullPointerException when properties is null")
  void constructorThrowsWhenPropertiesNull() {
    var catalogManager = mock(CatalogModelManager.class);
    var whisper = mock(WhisperTranscriptionClient.class);
    assertThrows(
        NullPointerException.class, () -> new LocalAudioProcessor(null, catalogManager, whisper));
  }

  @Test
  @DisplayName("Constructor throws NullPointerException when whisperClient is null")
  void constructorThrowsWhenWhisperNull() {
    var catalogManager = mock(CatalogModelManager.class);
    var props = propsWithAudio();
    assertThrows(
        NullPointerException.class, () -> new LocalAudioProcessor(props, catalogManager, null));
  }

  @Test
  @DisplayName("process throws NullPointerException when audioBytes is null")
  void processThrowsWhenBytesNull() {
    var catalogManager = mock(CatalogModelManager.class);
    var whisper = mock(WhisperTranscriptionClient.class);
    var processor = new LocalAudioProcessor(propsWithAudio(), catalogManager, whisper);

    assertThrows(NullPointerException.class, () -> processor.process(null));
  }

  @Test
  @DisplayName("process returns empty payload when audioBytes is empty")
  void processReturnsEmptyWhenBytesEmpty() {
    var catalogManager = mock(CatalogModelManager.class);
    var whisper = mock(WhisperTranscriptionClient.class);
    var processor = new LocalAudioProcessor(propsWithAudio(), catalogManager, whisper);

    ProcessedAudioPayload payload = processor.process(new byte[0]);
    assertNotNull(payload);
    assertEquals("", payload.transcript());
    verifyNoInteractions(whisper);
  }

  @Test
  @DisplayName("process returns empty payload when audioBytes is all zeros")
  void processReturnsEmptyWhenBytesAllZeros() {
    var catalogManager = mock(CatalogModelManager.class);
    var whisper = mock(WhisperTranscriptionClient.class);
    var processor = new LocalAudioProcessor(propsWithAudio(), catalogManager, whisper);

    ProcessedAudioPayload payload = processor.process(new byte[100]);
    assertNotNull(payload);
    assertEquals("", payload.transcript());
    verifyNoInteractions(whisper);
  }

  @Test
  @DisplayName("process returns transcribed text on success")
  void processReturnsTranscriptOnSuccess() {
    var catalogManager = mock(CatalogModelManager.class);
    var whisper = mock(WhisperTranscriptionClient.class);
    when(whisper.transcribe(any(), any(), any(), any())).thenReturn("mocked transcription");

    var processor = new LocalAudioProcessor(propsWithAudio(), catalogManager, whisper);
    // Non-zero bytes to bypass all-zeros check
    byte[] audioBytes = new byte[] {1, 2, 3};

    ProcessedAudioPayload payload = processor.process(audioBytes);
    assertNotNull(payload);
    assertEquals("mocked transcription", payload.transcript());
    verify(whisper, times(1)).transcribe(any(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "process handles default audio model fallback when configuration lacks transcription model")
  void processUsesFallbackModel() {
    var catalogManager = mock(CatalogModelManager.class);
    var whisper = mock(WhisperTranscriptionClient.class);
    when(whisper.transcribe(any(), any(), any(), any())).thenReturn("fallback text");

    var processor = new LocalAudioProcessor(propsWithNullAudio(), catalogManager, whisper);
    byte[] audioBytes = new byte[] {1, 2, 3};

    ProcessedAudioPayload payload = processor.process(audioBytes);
    assertNotNull(payload);
    assertEquals("fallback text", payload.transcript());
    verify(whisper, times(1)).transcribe(any(), any(), eq("speech.mp3"), eq("whisper-1"));
  }

  @Test
  @DisplayName(
      "process throws UncheckedIOException when Whisper throws exception with IOException cause")
  void processThrowsUncheckedIOException() {
    var catalogManager = mock(CatalogModelManager.class);
    var whisper = mock(WhisperTranscriptionClient.class);
    IOException ioCause = new IOException("socket closed");
    when(whisper.transcribe(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("failed", ioCause));

    var processor = new LocalAudioProcessor(propsWithAudio(), catalogManager, whisper);
    byte[] audioBytes = new byte[] {1, 2, 3};

    assertThrows(UncheckedIOException.class, () -> processor.process(audioBytes));
  }

  @Test
  @DisplayName("process propagates original RuntimeException when cause is not IOException")
  void processPropagatesOriginalException() {
    var catalogManager = mock(CatalogModelManager.class);
    var whisper = mock(WhisperTranscriptionClient.class);
    when(whisper.transcribe(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("custom failure"));

    var processor = new LocalAudioProcessor(propsWithAudio(), catalogManager, whisper);
    byte[] audioBytes = new byte[] {1, 2, 3};

    var ex = assertThrows(RuntimeException.class, () -> processor.process(audioBytes));
    assertEquals("custom failure", ex.getMessage());
  }
}
