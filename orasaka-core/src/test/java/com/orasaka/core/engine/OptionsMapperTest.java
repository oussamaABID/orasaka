package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.support.DefaultOptions;
import com.orasaka.core.support.InternalImageRequest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageOptions;

/** Unit tests for {@link OptionsMapper} — provider-aware options mapping. */
class OptionsMapperTest {

  @Nested
  @DisplayName("mapOptions — chat options")
  class MapChatOptions {

    @Test
    @DisplayName("null options defaults to temperature 0.7")
    void nullOptionsDefaults() {
      var result = OptionsMapper.mapOptions(null, "ollama");
      assertInstanceOf(OllamaChatOptions.class, result);
    }

    @Test
    @DisplayName("ollama provider returns OllamaChatOptions")
    void ollamaProvider() {
      var opts = new DefaultOptions(0.5, 100, Map.of());
      var result = OptionsMapper.mapOptions(opts, "ollama");
      assertInstanceOf(OllamaChatOptions.class, result);
    }

    @Test
    @DisplayName("openai provider returns OpenAiChatOptions")
    void openaiProvider() {
      var opts = new DefaultOptions(0.3, 200, Map.of());
      var result = OptionsMapper.mapOptions(opts, "openai");
      assertInstanceOf(OpenAiChatOptions.class, result);
    }

    @Test
    @DisplayName("OPENAI case insensitive returns OpenAiChatOptions")
    void openaiCaseInsensitive() {
      var result = OptionsMapper.mapOptions(null, "OPENAI");
      assertInstanceOf(OpenAiChatOptions.class, result);
    }

    @Test
    @DisplayName("unknown provider returns DefaultChatOptions")
    void unknownProvider() {
      var result = OptionsMapper.mapOptions(null, "anthropic");
      assertInstanceOf(DefaultChatOptions.class, result);
    }

    @Test
    @DisplayName("null temperature in DefaultOptions defaults to 0.7")
    void nullTemperatureDefaults() {
      var opts = new DefaultOptions(null, null, Map.of());
      var result = OptionsMapper.mapOptions(opts, "openai");
      assertNotNull(result);
    }

    @Test
    @DisplayName("empty DefaultOptions via no-arg constructor")
    void emptyDefaultOptions() {
      var opts = new DefaultOptions();
      var result = OptionsMapper.mapOptions(opts, "ollama");
      assertInstanceOf(OllamaChatOptions.class, result);
    }
  }

  @Nested
  @DisplayName("mapImageOptions")
  class MapImageOptions {

    @Test
    @DisplayName("openai provider returns OpenAiImageOptions")
    void openaiImageOptions() {
      var request = new InternalImageRequest("a cat", 512, 512, null, null);
      var result = OptionsMapper.mapImageOptions(request, "openai");
      assertInstanceOf(OpenAiImageOptions.class, result);
    }

    @Test
    @DisplayName("unknown provider returns null")
    void unknownImageProvider() {
      var request = new InternalImageRequest("a cat", 512, 512, null, null);
      var result = OptionsMapper.mapImageOptions(request, "ollama");
      assertNull(result);
    }
  }
}
