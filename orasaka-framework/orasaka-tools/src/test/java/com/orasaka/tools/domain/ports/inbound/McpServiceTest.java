package com.orasaka.tools.domain.ports.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.orasaka.core.infrastructure.config.CoreProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link McpService} initialization and context resolution. */
class McpServiceTest {

  @Test
  void shouldInitializeWithEndpoints() {
    // Given
    CoreProperties.McpConfig mcpProps =
        new CoreProperties.McpConfig(List.of("http://localhost:8080/mcp"));
    CoreProperties props =
        new CoreProperties(
            "ollama",
            null,
            mcpProps,
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null,
            null,
            null,
            null);

    // When
    McpService service = new McpService(props);

    // Then
    assertThat(service).isNotNull();
  }
}
