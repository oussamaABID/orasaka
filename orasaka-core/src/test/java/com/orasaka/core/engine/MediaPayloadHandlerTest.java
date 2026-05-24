package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.support.InternalImageRequest;
import com.orasaka.core.support.InternalSpeechRequest;
import com.orasaka.core.support.OrasakaContext;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MediaPayloadHandler} pure-logic utilities. */
class MediaPayloadHandlerTest {

  @Nested
  @DisplayName("resolveVoicePreference()")
  class VoicePreference {

    @Test
    @DisplayName("returns user preference when present")
    void returnsPreference() {
      var ctx = new OrasakaContext("u", "c", Map.of("tts-voice", "nova"), Set.of());
      assertEquals("nova", MediaPayloadHandler.resolveVoicePreference(ctx));
    }

    @Test
    @DisplayName("returns 'alloy' when context is null")
    void defaultOnNull() {
      assertEquals("alloy", MediaPayloadHandler.resolveVoicePreference(null));
    }

    @Test
    @DisplayName("returns 'alloy' when preferences lack tts-voice")
    void defaultWhenMissing() {
      var ctx = new OrasakaContext("u", "c", Map.of(), Set.of());
      assertEquals("alloy", MediaPayloadHandler.resolveVoicePreference(ctx));
    }
  }

  @Nested
  @DisplayName("toSpeechPrompt()")
  class SpeechPrompt {

    @Test
    @DisplayName("creates speech prompt from request")
    void createsSpeechPrompt() {
      var request = new InternalSpeechRequest("Hello world", null, null);
      var prompt = MediaPayloadHandler.toSpeechPrompt(request, "alloy");
      assertNotNull(prompt);
    }
  }

  @Nested
  @DisplayName("toImagePrompt()")
  class ImagePrompt {

    @Test
    @DisplayName("creates image prompt with openai provider")
    void createsImagePrompt() {
      var request = new InternalImageRequest("a cat", 512, 512, null, null);
      var prompt = MediaPayloadHandler.toImagePrompt(request, "openai");
      assertNotNull(prompt);
      assertFalse(prompt.getInstructions().isEmpty());
    }

    @Test
    @DisplayName("creates image prompt with null options for unsupported provider")
    void createsWithNullOptions() {
      var request = new InternalImageRequest("a dog", 256, 256, null, null);
      var prompt = MediaPayloadHandler.toImagePrompt(request, "ollama");
      assertNotNull(prompt);
    }
  }
}
