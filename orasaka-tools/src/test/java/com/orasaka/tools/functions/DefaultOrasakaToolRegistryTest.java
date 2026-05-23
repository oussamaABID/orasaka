package com.orasaka.tools.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.orasaka.core.interceptors.tool.OrasakaToolRegistry;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

class DefaultOrasakaToolRegistryTest {

  private OrasakaToolRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new DefaultOrasakaToolRegistry();
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
    assertThat(tools).hasSize(1);
    assertThat(tools.getFirst().getToolDefinition().name()).isEqualTo(name);
    assertThat(tools.getFirst().getToolDefinition().description()).isEqualTo(description);
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
