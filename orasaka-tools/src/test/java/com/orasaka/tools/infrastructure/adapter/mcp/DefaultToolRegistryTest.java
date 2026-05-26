package com.orasaka.tools.infrastructure.adapter.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.orasaka.core.domain.ports.outbound.ToolRegistry;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/** Unit tests for {@link DefaultToolRegistry} registration and retrieval. */
class DefaultToolRegistryTest {

  private ToolRegistry registry;

  @BeforeEach
  void setUp() {
    DefaultToolRegistry reg = new DefaultToolRegistry(null, null, null, null);
    reg.registerDefaultTools();
    registry = reg;
  }

  @Test
  void shouldRegisterTool() {
    // Given
    String name = "testTool";
    String description = "A test tool";
    Function<String, String> function = input -> "Hello " + input;

    // When
    registry.registerTool(name, description, String.class, function);

    // Then
    List<ToolCallback> tools = registry.getRegisteredTools();
    assertThat(tools).hasSize(4);
    assertThat(tools.stream().map(t -> t.getToolDefinition().name()))
        .contains(name, "analyzePoster", "analyzeAudioExtract", "searchWeb");
  }

  @Test
  void shouldReturnImmutableList() {
    // Given
    registry.registerTool("tool1", "desc1", String.class, (String s) -> s);

    // When
    List<ToolCallback> tools = registry.getRegisteredTools();

    // Then
    assertThrows(UnsupportedOperationException.class, () -> tools.add(null));
  }
}
