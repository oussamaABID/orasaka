package com.orasaka.tools.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.orasaka.core.engine.CoreProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrasakaMcpServiceTest {

  @Test
  void shouldInitializeWithEndpoints() {
    // Given
    CoreProperties.McpConfig mcpProps =
        new CoreProperties.McpConfig(List.of("http://localhost:8080/mcp"));
    CoreProperties props =
        new CoreProperties(
            "ollama",
            Map.of(),
            null,
            mcpProps,
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null);

    // When
    OrasakaMcpService service = new OrasakaMcpService(props);

    // Then
    assertThat(service).isNotNull();
  }
}
