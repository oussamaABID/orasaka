package com.orasaka.gateway.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MediaContracts} — media ingestion request validation. */
class MediaContractsTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() {}


  @Nested
  @DisplayName("AnalyzePosterRequest")
  class AnalyzePosterTests {

    @Test
    @DisplayName("valid UUID poster accepted")
    void validPoster() {
      UUID assetId = UUID.randomUUID();
      var req = new MediaContracts.AnalyzePosterRequest(assetId, "describe");
      assertEquals(assetId, req.assetId());
      assertEquals("describe", req.prompt());
    }

    @Test
    @DisplayName("null assetId throws InvalidRequestException")
    void nullPoster() {
      assertThrows(
          InvalidRequestException.class,
          () -> new MediaContracts.AnalyzePosterRequest(null, "describe"));
    }

    @Test
    @DisplayName("null prompt accepted")
    void nullPromptAccepted() {
      UUID assetId = UUID.randomUUID();
      var req = new MediaContracts.AnalyzePosterRequest(assetId, null);
      assertNull(req.prompt());
    }
  }

  @Nested
  @DisplayName("AnalyzeAudioRequest")
  class AnalyzeAudioTests {

    @Test
    @DisplayName("valid audio asset UUID accepted")
    void validAudio() {
      UUID assetId = UUID.randomUUID();
      var req = new MediaContracts.AnalyzeAudioRequest(assetId, "thread-1");
      assertEquals(assetId, req.assetId());
      assertEquals("thread-1", req.threadId());
    }

    @Test
    @DisplayName("null assetId throws InvalidRequestException")
    void nullAudio() {
      assertThrows(
          InvalidRequestException.class,
          () -> new MediaContracts.AnalyzeAudioRequest(null, "thread-1"));
    }
  }
}
