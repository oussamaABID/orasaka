package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class JobMessageTest {

  @Test
  void fullConstructor_setsAllFields() {
    var msg = new JobMessage("job-1", "user-1", "video_gen", "gpt-4", Map.of("key", "value"));
    assertEquals("job-1", msg.jobId());
    assertEquals("user-1", msg.userId());
    assertEquals("video_gen", msg.featureKey());
    assertEquals("gpt-4", msg.model());
    assertEquals(Map.of("key", "value"), msg.payload());
  }

  @Test
  void backwardsCompatibleConstructor_setsDefaultModel() {
    var msg = new JobMessage("job-1", "user-1", "image_gen", Map.of("prompt", "test"));
    assertEquals("default", msg.model());
  }

  @Test
  void nullJobId_throws() {
    Map<String, Object> emptyMap = Map.of();
    assertThrows(
        NullPointerException.class,
        () -> new JobMessage(null, "user-1", "feat", "model", emptyMap));
  }

  @Test
  void nullFeatureKey_throws() {
    Map<String, Object> emptyMap = Map.of();
    assertThrows(
        NullPointerException.class,
        () -> new JobMessage("job-1", "user-1", null, "model", emptyMap));
  }

  @Test
  void nullModel_throws() {
    Map<String, Object> emptyMap = Map.of();
    assertThrows(
        NullPointerException.class,
        () -> new JobMessage("job-1", "user-1", "feat", null, emptyMap));
  }

  @Test
  void nullPayload_defaultsToEmptyMap() {
    var msg = new JobMessage("job-1", "user-1", "feat", "model", null);
    assertNotNull(msg.payload());
    assertTrue(msg.payload().isEmpty());
  }

  @Test
  void payload_isDefensivelyCopied() {
    var original = new java.util.HashMap<String, Object>();
    original.put("key", "value");
    var msg = new JobMessage("job-1", "user-1", "feat", "model", original);
    var payload = msg.payload();
    assertThrows(UnsupportedOperationException.class, () -> payload.put("new", "val"));
  }
}
