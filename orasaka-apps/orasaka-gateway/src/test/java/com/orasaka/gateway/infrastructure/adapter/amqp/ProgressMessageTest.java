package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProgressMessageTest {

  @Test
  void validConstruction_setsFields() {
    var msg = new ProgressMessage("job-1", 50);
    assertEquals("job-1", msg.jobId());
    assertEquals(50, msg.progress());
  }

  @Test
  void nullJobId_throws() {
    assertThrows(NullPointerException.class, () -> new ProgressMessage(null, 50));
  }

  @Test
  void nullProgress_throws() {
    assertThrows(NullPointerException.class, () -> new ProgressMessage("job-1", null));
  }

  @Test
  void negativeProgress_throws() {
    assertThrows(IllegalArgumentException.class, () -> new ProgressMessage("job-1", -1));
  }

  @Test
  void overHundredProgress_throws() {
    assertThrows(IllegalArgumentException.class, () -> new ProgressMessage("job-1", 101));
  }

  @Test
  void zeroProgress_valid() {
    var msg = new ProgressMessage("job-1", 0);
    assertEquals(0, msg.progress());
  }

  @Test
  void hundredProgress_valid() {
    var msg = new ProgressMessage("job-1", 100);
    assertEquals(100, msg.progress());
  }
}
