package com.orasaka.core.application.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.infrastructure.config.OllamaOptionsStrategy;
import com.orasaka.core.infrastructure.config.OpenAiOptionsStrategy;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

class PipelineOptionsRegistryTest {

  private final PipelineOptionsRegistry registry;

  public PipelineOptionsRegistryTest() throws Exception {
    Constructor<OllamaOptionsStrategy> ollamaConstructor =
        OllamaOptionsStrategy.class.getDeclaredConstructor();
    ollamaConstructor.setAccessible(true);
    OllamaOptionsStrategy ollamaStrategy = ollamaConstructor.newInstance();

    Constructor<OpenAiOptionsStrategy> openaiConstructor =
        OpenAiOptionsStrategy.class.getDeclaredConstructor();
    openaiConstructor.setAccessible(true);
    OpenAiOptionsStrategy openaiStrategy = openaiConstructor.newInstance();

    this.registry = new PipelineOptionsRegistry(List.of(ollamaStrategy, openaiStrategy));
  }

  @Test
  @DisplayName("builds OllamaChatOptions for 'ollama' provider")
  void buildsOllamaOptions() {
    ChatOptions result = registry.build("ollama", "llama3:8b", 0.3);
    assertInstanceOf(OllamaChatOptions.class, result);
    var ollama = (OllamaChatOptions) result;
    assertEquals("llama3:8b", ollama.getModel());
    assertEquals(0.3, ollama.getTemperature());
  }

  @Test
  @DisplayName("builds OpenAiChatOptions for 'openai' provider")
  void buildsOpenAiOptions() {
    ChatOptions result = registry.build("openai", "gpt-4o", 0.5);
    assertInstanceOf(OpenAiChatOptions.class, result);
    var openai = (OpenAiChatOptions) result;
    assertEquals("gpt-4o", openai.getModel());
    assertEquals(0.5, openai.getTemperature());
  }

  @Test
  @DisplayName("throws IllegalArgumentException for unknown provider")
  void throwsForUnknown() {
    assertThrows(IllegalArgumentException.class, () -> registry.build("anthropic", "claude", 0.7));
  }

  @Test
  @DisplayName("provider match is case-insensitive")
  void caseInsensitive() {
    ChatOptions result = registry.build("OLLAMA", "model", 0.5);
    assertInstanceOf(OllamaChatOptions.class, result);
  }
}
