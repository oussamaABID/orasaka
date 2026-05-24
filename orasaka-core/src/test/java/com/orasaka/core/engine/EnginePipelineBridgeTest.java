package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.support.Context;
import com.orasaka.core.support.InternalChatRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Unit tests for {@link EnginePipelineBridge} covering context compilation, message mapping, and
 * tool removal.
 */
class EnginePipelineBridgeTest {

  private static final CoreProperties PROPS =
      new CoreProperties("ollama", Map.of(), null, null, null, null);

  private static EngineModelRegistry stubRegistry() {
    return new EngineModelRegistry(Map.of(), Map.of(), Map.of(), Map.of(), PROPS);
  }

  @Nested
  @DisplayName("mapMessage()")
  class MapMessage {

    @Test
    @DisplayName("maps 'system' role to SystemMessage")
    void mapsSystem() {
      Message result =
          EnginePipelineBridge.mapMessage(new InternalChatRequest.ChatMessage("system", "hello"));
      assertInstanceOf(SystemMessage.class, result);
      assertEquals("hello", result.getText());
    }

    @Test
    @DisplayName("maps 'assistant' role to AssistantMessage")
    void mapsAssistant() {
      Message result =
          EnginePipelineBridge.mapMessage(
              new InternalChatRequest.ChatMessage("assistant", "response"));
      assertInstanceOf(AssistantMessage.class, result);
    }

    @Test
    @DisplayName("maps 'user' role to UserMessage")
    void mapsUser() {
      Message result =
          EnginePipelineBridge.mapMessage(new InternalChatRequest.ChatMessage("user", "question"));
      assertInstanceOf(UserMessage.class, result);
    }

    @Test
    @DisplayName("maps unknown role to UserMessage (default)")
    void mapsUnknown() {
      Message result =
          EnginePipelineBridge.mapMessage(new InternalChatRequest.ChatMessage("custom", "data"));
      assertInstanceOf(UserMessage.class, result);
    }
  }

  @Nested
  @DisplayName("compileContext()")
  class CompileContext {

    @Test
    @DisplayName("compiles context without pipeline (null pipeline)")
    void compilesWithoutPipeline() {
      var request = InternalChatRequest.simple("test prompt");
      var result = EnginePipelineBridge.compileContext(request, null, stubRegistry(), List.of());

      assertEquals("ollama", result.provider());
      assertEquals("test prompt", result.promptText());
      assertNotNull(result.toPrompt());
    }

    @Test
    @DisplayName("compiles context with conversation ID")
    void compilesWithConversationId() {
      var ctx = new Context("user1", "conv-123", Map.of(), Set.of());
      var request = new InternalChatRequest("test", List.of(), null, ctx);
      var result = EnginePipelineBridge.compileContext(request, null, stubRegistry(), List.of());

      assertEquals("conv-123", result.conversationId());
    }

    @Test
    @DisplayName("conversation ID is null when context is null")
    void conversationIdNullWhenNoContext() {
      var request = InternalChatRequest.simple("test");
      var result = EnginePipelineBridge.compileContext(request, null, stubRegistry(), List.of());

      assertNull(result.conversationId());
    }
  }

  @Nested
  @DisplayName("removeTools()")
  class RemoveTools {

    @Test
    @DisplayName("returns same prompt when options is null")
    void returnsPromptWhenNoOptions() {
      var prompt = new org.springframework.ai.chat.prompt.Prompt("test");
      var result = EnginePipelineBridge.removeTools(prompt);
      assertSame(prompt, result);
    }
  }
}
