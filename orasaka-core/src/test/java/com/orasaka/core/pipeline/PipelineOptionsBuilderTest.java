package com.orasaka.core.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

/** Unit tests for {@link PipelineOptionsBuilder} covering all provider path branches. */
class PipelineOptionsBuilderTest {

  @Test
  @DisplayName("builds OllamaChatOptions for 'ollama' provider")
  void buildsOllamaOptions() {
    ChatOptions result = PipelineOptionsBuilder.build("ollama", "llama3:8b", 0.3);
    assertInstanceOf(OllamaChatOptions.class, result);
    var ollama = (OllamaChatOptions) result;
    assertEquals("llama3:8b", ollama.getModel());
    assertEquals(0.3, ollama.getTemperature());
  }

  @Test
  @DisplayName("builds OpenAiChatOptions for 'openai' provider")
  void buildsOpenAiOptions() {
    ChatOptions result = PipelineOptionsBuilder.build("openai", "gpt-4o", 0.5);
    assertInstanceOf(OpenAiChatOptions.class, result);
    var openai = (OpenAiChatOptions) result;
    assertEquals("gpt-4o", openai.getModel());
    assertEquals(0.5, openai.getTemperature());
  }

  @Test
  @DisplayName("returns null for unknown provider")
  void returnsNullForUnknown() {
    assertNull(PipelineOptionsBuilder.build("anthropic", "claude", 0.7));
  }

  @Test
  @DisplayName("provider match is case-insensitive")
  void caseInsensitive() {
    ChatOptions result = PipelineOptionsBuilder.build("OLLAMA", "model", 0.5);
    assertInstanceOf(OllamaChatOptions.class, result);
  }
}
