package com.orasaka.gateway.support;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.exception.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MediaContracts} — media ingestion request validation. */
class MediaContractsTest {

  @Nested
  @DisplayName("AnalyzePosterRequest")
  class AnalyzePosterTests {

    @Test
    @DisplayName("valid base64 poster accepted")
    void validPoster() {
      var req = new MediaContracts.AnalyzePosterRequest("base64data", "describe");
      assertEquals("base64data", req.posterBase64());
      assertEquals("describe", req.prompt());
    }

    @Test
    @DisplayName("null posterBase64 throws InvalidRequestException")
    void nullPoster() {
      assertThrows(
          InvalidRequestException.class,
          () -> new MediaContracts.AnalyzePosterRequest(null, "describe"));
    }

    @Test
    @DisplayName("blank posterBase64 throws InvalidRequestException")
    void blankPoster() {
      assertThrows(
          InvalidRequestException.class,
          () -> new MediaContracts.AnalyzePosterRequest("  ", "describe"));
    }

    @Test
    @DisplayName("null prompt accepted")
    void nullPromptAccepted() {
      var req = new MediaContracts.AnalyzePosterRequest("base64data", null);
      assertNull(req.prompt());
    }
  }

  @Nested
  @DisplayName("AnalyzeAudioRequest")
  class AnalyzeAudioTests {

    @Test
    @DisplayName("valid audio base64 accepted")
    void validAudio() {
      var req = new MediaContracts.AnalyzeAudioRequest("audioBase64", "thread-1");
      assertEquals("audioBase64", req.audioBase64());
      assertEquals("thread-1", req.threadId());
    }

    @Test
    @DisplayName("null audioBase64 throws InvalidRequestException")
    void nullAudio() {
      assertThrows(
          InvalidRequestException.class,
          () -> new MediaContracts.AnalyzeAudioRequest(null, "thread-1"));
    }

    @Test
    @DisplayName("blank audioBase64 throws InvalidRequestException")
    void blankAudio() {
      assertThrows(
          InvalidRequestException.class,
          () -> new MediaContracts.AnalyzeAudioRequest("  ", "thread-1"));
    }
  }
}
