package com.orasaka.interceptor.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.orasaka.core.domain.model.PromptContext;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemContextInjectorTest {

  @Test
  @DisplayName("intercept injects system metadata and trends into PromptContext")
  void injectsSystemMetadata() {
    SystemContextInjector injector = new SystemContextInjector();
    PromptContext context = new PromptContext("query", Map.of());

    PromptContext result = injector.intercept(context);

    assertThat(result).isNotNull();
    assertThat(result.systemMetadata())
        .containsEntry("systemStatus", "OPERATIONAL")
        .containsEntry("activeTools", "searchWeb, ttsGenerator, imageGenerator")
        .containsEntry("activeTrends", "AI-agentic-flows, virtual-threads-concurrency");
  }
}
