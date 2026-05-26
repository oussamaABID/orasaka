package com.orasaka.core.domain.model.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class McpToolInfoTest {

  @Test
  void shouldConstructMcpToolInfoSuccessfully() {
    McpToolInfo info = new McpToolInfo("toolName", "description text", "{\"type\":\"object\"}");
    assertThat(info.name()).isEqualTo("toolName");
    assertThat(info.description()).isEqualTo("description text");
    assertThat(info.inputSchema()).isEqualTo("{\"type\":\"object\"}");
  }

  @Test
  void shouldThrowWhenConstructorArgumentsAreNull() {
    assertThatThrownBy(() -> new McpToolInfo(null, "desc", "schema"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("name must not be null");

    assertThatThrownBy(() -> new McpToolInfo("name", null, "schema"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("description must not be null");

    assertThatThrownBy(() -> new McpToolInfo("name", "desc", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("inputSchema must not be null");
  }
}
