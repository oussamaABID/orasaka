package com.orasaka.core.pipeline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.support.InternalChatRequest;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/** Unit tests for {@link ToolInterceptor} — demand detection and tool attachment. */
class ToolInterceptorTest {

  @Nested
  @DisplayName("No tools available")
  class NoTools {

    @Test
    @DisplayName("returns options unchanged when registry is null")
    void nullRegistry() {
      var interceptor = new ToolInterceptor(null);
      var options = OllamaChatOptions.builder().model("llama3").build();
      var request = InternalChatRequest.simple("hello");

      var result = interceptor.preProcess(request, "hello", List.of(), options);
      assertSame(options, result);
    }

    @Test
    @DisplayName("returns options unchanged when registry is empty")
    void emptyRegistry() {
      var registry = mock(ToolRegistry.class);
      when(registry.getRegisteredTools()).thenReturn(Collections.emptyList());
      var interceptor = new ToolInterceptor(registry);
      var options = OllamaChatOptions.builder().model("llama3").build();
      var request = InternalChatRequest.simple("hello");

      var result = interceptor.preProcess(request, "hello", List.of(), options);
      assertSame(options, result);
    }
  }

  @Nested
  @DisplayName("Demand detection")
  class DemandDetection {

    private ToolCallback createMockTool(String name) {
      var tool = mock(ToolCallback.class);
      var toolDef = mock(ToolDefinition.class);
      when(toolDef.name()).thenReturn(name);
      when(tool.getToolDefinition()).thenReturn(toolDef);
      return tool;
    }

    @Test
    @DisplayName("poster keyword triggers analyzePoster tool")
    void posterKeyword() {
      var posterTool = createMockTool("analyzePoster");
      var registry = mock(ToolRegistry.class);
      when(registry.getRegisteredTools()).thenReturn(List.of(posterTool));
      var interceptor = new ToolInterceptor(registry);
      var options = OllamaChatOptions.builder().model("llama3").temperature(0.7).build();
      var request = InternalChatRequest.simple("analyze this poster");

      var result = interceptor.preProcess(request, "analyze this poster", List.of(), options);
      assertNotSame(options, result);
    }

    @Test
    @DisplayName("audio keyword triggers analyzeAudioExtract tool")
    void audioKeyword() {
      var audioTool = createMockTool("analyzeAudioExtract");
      var registry = mock(ToolRegistry.class);
      when(registry.getRegisteredTools()).thenReturn(List.of(audioTool));
      var interceptor = new ToolInterceptor(registry);
      var options = OllamaChatOptions.builder().model("llama3").temperature(0.7).build();
      var request = InternalChatRequest.simple("extract audio from clip");

      var result = interceptor.preProcess(request, "extract audio from clip", List.of(), options);
      assertNotSame(options, result);
    }

    @Test
    @DisplayName("generic tool is always attached")
    void genericToolAlwaysAttached() {
      var genericTool = createMockTool("searchWeb");
      var registry = mock(ToolRegistry.class);
      when(registry.getRegisteredTools()).thenReturn(List.of(genericTool));
      var interceptor = new ToolInterceptor(registry);
      var options = OllamaChatOptions.builder().model("llama3").temperature(0.7).build();
      var request = InternalChatRequest.simple("random question");

      var result = interceptor.preProcess(request, "random question", List.of(), options);
      assertNotSame(options, result);
    }

    @Test
    @DisplayName("poster tool is NOT attached when no visual keyword present")
    void posterToolNotAttachedWithoutKeyword() {
      var posterTool = createMockTool("analyzePoster");
      var registry = mock(ToolRegistry.class);
      when(registry.getRegisteredTools()).thenReturn(List.of(posterTool));
      var interceptor = new ToolInterceptor(registry);
      var options = OllamaChatOptions.builder().model("llama3").temperature(0.7).build();
      var request = InternalChatRequest.simple("tell me a joke");

      var result = interceptor.preProcess(request, "tell me a joke", List.of(), options);
      assertSame(options, result);
    }
  }

  @Nested
  @DisplayName("Options attachment")
  class OptionsAttachment {

    private ToolCallback createMockTool(String name) {
      var tool = mock(ToolCallback.class);
      var toolDef = mock(ToolDefinition.class);
      when(toolDef.name()).thenReturn(name);
      when(tool.getToolDefinition()).thenReturn(toolDef);
      return tool;
    }

    @Test
    @DisplayName("attaches tools to OpenAiChatOptions")
    void openAiOptions() {
      var genericTool = createMockTool("searchWeb");
      var registry = mock(ToolRegistry.class);
      when(registry.getRegisteredTools()).thenReturn(List.of(genericTool));
      var interceptor = new ToolInterceptor(registry);
      var options = OpenAiChatOptions.builder().model("gpt-4o").temperature(0.5).build();
      var request = InternalChatRequest.simple("hello");

      var result = interceptor.preProcess(request, "hello", List.of(), options);
      assertInstanceOf(OpenAiChatOptions.class, result);
    }

    @Test
    @DisplayName("returns original options for unknown options type")
    void unknownOptionsType() {
      var genericTool = createMockTool("searchWeb");
      var registry = mock(ToolRegistry.class);
      when(registry.getRegisteredTools()).thenReturn(List.of(genericTool));
      var interceptor = new ToolInterceptor(registry);
      var options = mock(ChatOptions.class);
      var request = InternalChatRequest.simple("hello");

      var result = interceptor.preProcess(request, "hello", List.of(), options);
      assertSame(options, result);
    }
  }
}
