package com.orasaka.gateway.infrastructure.adapter.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Test-only simulation hook that injects an artificial delay when a job prompt starts with
 * "TIMEOUT". Only activated when {@code orasaka.jobs.enable-test-simulations=true}.
 *
 * <p>This class isolates {@link Thread#sleep} from production code to satisfy SonarQube DoS
 * security hotspot rules (squid:S2925).
 *
 * @see JobSimulationHook
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "orasaka.jobs.enable-test-simulations", havingValue = "true")
class TimeoutSimulationHook implements JobSimulationHook {

  private static final Logger logger = LoggerFactory.getLogger(TimeoutSimulationHook.class);

  @Override
  public void beforeExecution(JobMessage message, int timeoutSeconds) {
    String prompt = (String) message.payload().get("prompt");
    if (prompt == null) {
      prompt = (String) message.payload().get("text");
    }
    if (prompt != null && prompt.startsWith("TIMEOUT")) {
      try {
        logger.info("Simulating hanging task timeout for job: {}", message.jobId());
        Thread.sleep((timeoutSeconds + 10) * 1000L); // NOSONAR — intentional test simulation
      } catch (InterruptedException ie) {
        logger.info("Simulated timeout sleep interrupted.");
        Thread.currentThread().interrupt();
      }
    }
  }
}
