package com.orasaka.automation.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orasaka.automation.domain.model.AutomationJobPayload;
import com.orasaka.automation.domain.model.AutomationJobStatus;
import com.orasaka.automation.infrastructure.config.AmqpConstants;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Tests for {@link ConnectorDispatcher} routing logic.
 *
 * <p>Validates all connector types are correctly dispatched, unknown types are rejected, and CLI
 * agent payloads are published to the correct AMQP routing key.
 */
@ExtendWith(MockitoExtension.class)
class ConnectorDispatcherTest {

  @Mock private RabbitTemplate rabbitTemplate;
  private ConnectorDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    dispatcher = new ConnectorDispatcher(rabbitTemplate);
  }

  private AutomationJobPayload createJob(String connectorType) {
    return new AutomationJobPayload(
        "job-001",
        "user-42",
        connectorType,
        "create-issue",
        AutomationJobStatus.RUNNING,
        Map.of("summary", "Test issue"),
        Instant.now());
  }

  @Nested
  @DisplayName("Connector Routing")
  class ConnectorRouting {

    @Test
    @DisplayName("Should dispatch JIRA connector without errors")
    void dispatchesJira() {
      assertThatCode(() -> dispatcher.dispatch(createJob("JIRA"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should dispatch WHATSAPP connector without errors")
    void dispatchesWhatsApp() {
      assertThatCode(() -> dispatcher.dispatch(createJob("WHATSAPP"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should dispatch MESSENGER connector without errors")
    void dispatchesMessenger() {
      assertThatCode(() -> dispatcher.dispatch(createJob("MESSENGER"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should dispatch SLACK connector without errors")
    void dispatchesSlack() {
      assertThatCode(() -> dispatcher.dispatch(createJob("SLACK"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle case-insensitive connector types")
    void caseInsensitiveConnectorType() {
      assertThatCode(() -> dispatcher.dispatch(createJob("jira"))).doesNotThrowAnyException();
      assertThatCode(() -> dispatcher.dispatch(createJob("Slack"))).doesNotThrowAnyException();
      assertThatCode(() -> dispatcher.dispatch(createJob("whatsApp"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject unknown connector type")
    void rejectsUnknownConnectorType() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> dispatcher.dispatch(createJob("UNKNOWN_PLATFORM")))
          .withMessageContaining("Unknown connector type");
    }
  }

  @Nested
  @DisplayName("CLI Agent Dispatch")
  class CliAgentDispatch {

    @Test
    @DisplayName("Should publish CLI_AGENT payload to AMQP with user-scoped routing key")
    void publishesCliAgentPayload() {
      dispatcher.dispatch(createJob("CLI_AGENT"));

      ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);

      verify(rabbitTemplate)
          .convertAndSend(exchangeCaptor.capture(), routingKeyCaptor.capture(), any(Map.class));

      assertThat(exchangeCaptor.getValue()).isEqualTo(AmqpConstants.AUTOMATION_EXCHANGE);
      assertThat(routingKeyCaptor.getValue()).isEqualTo("cli.user-42.dispatch");
    }

    @Test
    @DisplayName("CLI_AGENT payload should contain jobId, userId, action, and payload")
    @SuppressWarnings("unchecked")
    void cliPayloadContainsRequiredFields() {
      dispatcher.dispatch(createJob("CLI_AGENT"));

      ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
      verify(rabbitTemplate).convertAndSend(anyString(), anyString(), payloadCaptor.capture());

      Map<String, Object> sent = payloadCaptor.getValue();
      assertThat(sent)
          .containsKeys("jobId", "userId", "action", "payload")
          .containsEntry("jobId", "job-001")
          .containsEntry("userId", "user-42");
    }
  }
}
