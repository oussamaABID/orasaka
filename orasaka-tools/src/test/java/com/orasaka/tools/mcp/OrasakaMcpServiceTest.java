package com.orasaka.tools.mcp;

import com.orasaka.core.config.CoreProperties;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrasakaMcpServiceTest {

    @Test
    void shouldInitializeWithEndpoints() {
        // Given
        CoreProperties.McpConfig mcpProps = new CoreProperties.McpConfig(List.of("http://localhost:8080/mcp"));
        CoreProperties props = new CoreProperties("ollama", Map.of(), null, mcpProps);

        // When
        OrasakaMcpService service = new OrasakaMcpService(props);

        // Then
        assertThat(service).isNotNull();
    }
}
