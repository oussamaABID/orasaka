package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.support.DefaultOrasakaOptions;
import com.orasaka.core.support.InternalImageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageOptions;

/** Unit tests for {@link OrasakaOptionsMapper} covering all provider path branches. */
class OrasakaOptionsMapperTest {

  @Nested
  @DisplayName("mapOptions()")
  class MapChatOptions {

    @Test
    @DisplayName("maps null options to default temperature 0.7")
    void nullOptionsUseDefaults() {
      ChatOptions result = OrasakaOptionsMapper.mapOptions(null, "ollama");
      assertInstanceOf(OllamaChatOptions.class, result);
      assertEquals(0.7, ((OllamaChatOptions) result).getTemperature());
    }

    @Test
    @DisplayName("maps Ollama provider to OllamaChatOptions")
    void mapsOllama() {
      var opts = new DefaultOrasakaOptions(0.3, 512, null);
      ChatOptions result = OrasakaOptionsMapper.mapOptions(opts, "ollama");
      assertInstanceOf(OllamaChatOptions.class, result);
      assertEquals(0.3, ((OllamaChatOptions) result).getTemperature());
      assertEquals(512, ((OllamaChatOptions) result).getNumPredict());
    }

    @Test
    @DisplayName("maps OpenAI provider to OpenAiChatOptions")
    void mapsOpenAi() {
      var opts = new DefaultOrasakaOptions(0.5, 1024, null);
      ChatOptions result = OrasakaOptionsMapper.mapOptions(opts, "openai");
      assertInstanceOf(OpenAiChatOptions.class, result);
      assertEquals(0.5, ((OpenAiChatOptions) result).getTemperature());
      assertEquals(1024, ((OpenAiChatOptions) result).getMaxTokens());
    }

    @Test
    @DisplayName("maps unknown provider to DefaultChatOptions")
    void mapsUnknown() {
      var opts = new DefaultOrasakaOptions(0.9, null, null);
      ChatOptions result = OrasakaOptionsMapper.mapOptions(opts, "anthropic");
      assertInstanceOf(DefaultChatOptions.class, result);
      assertEquals(0.9, ((DefaultChatOptions) result).getTemperature());
    }

    @Test
    @DisplayName("DefaultOrasakaOptions with null temperature falls back to 0.7")
    void nullTempFallback() {
      var opts = new DefaultOrasakaOptions(null, null, null);
      ChatOptions result = OrasakaOptionsMapper.mapOptions(opts, "openai");
      assertEquals(0.7, ((OpenAiChatOptions) result).getTemperature());
    }

    @Test
    @DisplayName("provider match is case-insensitive")
    void caseInsensitive() {
      var opts = new DefaultOrasakaOptions(0.4, null, null);
      ChatOptions result = OrasakaOptionsMapper.mapOptions(opts, "OLLAMA");
      assertInstanceOf(OllamaChatOptions.class, result);
    }
  }

  @Nested
  @DisplayName("mapImageOptions()")
  class MapImageOptions {

    @Test
    @DisplayName("maps OpenAI provider to OpenAiImageOptions")
    void mapsOpenAiImage() {
      var request = new InternalImageRequest("test prompt", 512, 512, null, null);
      ImageOptions result = OrasakaOptionsMapper.mapImageOptions(request, "openai");
      assertInstanceOf(OpenAiImageOptions.class, result);
    }

    @Test
    @DisplayName("returns null for unsupported provider")
    void returnsNullForUnknown() {
      var request = new InternalImageRequest("test", 256, 256, null, null);
      assertNull(OrasakaOptionsMapper.mapImageOptions(request, "ollama"));
    }
  }
}
