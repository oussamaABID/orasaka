package com.orasaka.automation.infrastructure.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AmqpConstants} topology constants.
 *
 * <p>Validates exchange names, queue names, and routing keys match the AGENTS.md §2.14 contract.
 */
class AmqpConstantsTest {

  @Test
  @DisplayName("AUTOMATION_EXCHANGE should follow orasaka.automation.* pattern")
  void automationExchangePattern() {
    assertThat(AmqpConstants.AUTOMATION_EXCHANGE)
        .isNotBlank()
        .startsWith("orasaka.automation.")
        .isEqualTo("orasaka.automation.exchange");
  }

  @Test
  @DisplayName("JOBS_QUEUE should be durable queue name")
  void jobsQueueName() {
    assertThat(AmqpConstants.JOBS_QUEUE).isNotBlank().isEqualTo("orasaka.automation.jobs");
  }

  @Test
  @DisplayName("JOBS_ROUTING_KEY should match approved job dispatch pattern")
  void jobsRoutingKeyPattern() {
    assertThat(AmqpConstants.JOBS_ROUTING_KEY).isNotBlank().isEqualTo("job.approved");
  }

  @Test
  @DisplayName("TELEMETRY_ROUTING_KEY should match telemetry dispatch pattern")
  void telemetryRoutingKeyPattern() {
    assertThat(AmqpConstants.TELEMETRY_ROUTING_KEY).isNotBlank().isEqualTo("job.telemetry");
  }

  @Test
  @DisplayName("AmqpConstants private constructor should prevent instantiation")
  void notInstantiable() throws Exception {
    var ctor = AmqpConstants.class.getDeclaredConstructor();
    assertThat(java.lang.reflect.Modifier.isPrivate(ctor.getModifiers())).isTrue();
  }
}
