package com.orasaka.core.domain.model.video;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.Context;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoRequestTest {

  private final Context ctx = new Context("user", "conv", Map.of(), Set.of());

  @Test
  void fullConstruction() {
    var request =
        new VideoRequest("A cat", 6, "model", "job-1", "/input", "/output", Map.of(), ctx);
    assertEquals("A cat", request.prompt());
    assertEquals(6, request.durationSeconds());
    assertEquals("model", request.model());
    assertEquals("job-1", request.jobId());
    assertEquals("/input", request.inputPath());
    assertEquals("/output", request.outputPath());
  }

  @Test
  void nullDuration_defaultsTo4() {
    var request = new VideoRequest("prompt", null, null, null, null, null, null, ctx);
    assertEquals(4, request.durationSeconds());
  }

  @Test
  void nullOptionalFields_defaultToEmpty() {
    var request = new VideoRequest("prompt", null, null, null, null, null, null, ctx);
    assertEquals("", request.model());
    assertEquals("", request.jobId());
    assertEquals("", request.inputPath());
    assertEquals("", request.outputPath());
    assertTrue(request.settings().isEmpty());
  }

  @Test
  void shortConstructor_4args() {
    var request = new VideoRequest("prompt", 5, Map.of(), ctx);
    assertEquals("prompt", request.prompt());
    assertEquals(5, request.durationSeconds());
  }
}
