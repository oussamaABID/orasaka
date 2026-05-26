package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JobExecutionExceptionTest {

  @Test
  void constructWithMessage_carriesMessage() {
    var ex = new JobExecutionException("Job failed");
    assertEquals("Job failed", ex.getMessage());
    assertInstanceOf(Exception.class, ex);
  }

  @Test
  void constructWithMessageAndCause_carriesBoth() {
    var cause = new RuntimeException("root cause");
    var ex = new JobExecutionException("Job failed", cause);
    assertEquals("Job failed", ex.getMessage());
    assertSame(cause, ex.getCause());
  }
}
