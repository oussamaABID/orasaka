package com.orasaka.core.engine;

import com.orasaka.core.support.Context;
import com.orasaka.core.support.InternalImageRequest;
import com.orasaka.core.support.InternalSpeechRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;

/**
 * Utility class for constructing media-specific prompts (speech, image) from internal requests.
 *
 * <p>Handles voice preference resolution from the user's context preferences, and assembles Spring
 * AI {@link TextToSpeechPrompt} and {@link ImagePrompt} objects from the Orasaka-native request
 * models.
 *
 * @see AbstractEngine#generateSpeech(InternalSpeechRequest)
 * @since 1.0.0
 */
final class MediaPayloadHandler {

  /** Private constructor — utility class, not instantiable. */
  private MediaPayloadHandler() {}

  /**
   * Resolves the user's preferred TTS voice from their context preferences.
   *
   * <p>Looks for a {@code "tts-voice"} key in the user's preference map. Defaults to {@code
   * "alloy"} if the context is null or the preference is missing.
   *
   * @param context The user's execution context (nullable).
   * @return The resolved voice name string (never null).
   */
  static String resolveVoicePreference(Context context) {
    return Optional.ofNullable(context)
        .map(Context::preferences)
        .map(p -> p.get("tts-voice"))
        .map(Object::toString)
        .orElse("alloy");
  }

  /**
   * Constructs a Spring AI {@link TextToSpeechPrompt} from an internal speech request.
   *
   * @param request The internal speech request containing the text to synthesize.
   * @param voice The resolved voice name (currently unused by Spring AI TTS, reserved).
   * @return A configured {@link TextToSpeechPrompt} ready for model invocation.
   */
  static TextToSpeechPrompt toSpeechPrompt(InternalSpeechRequest request, String voice) {
    return new TextToSpeechPrompt(request.text());
  }

  /**
   * Constructs a Spring AI {@link ImagePrompt} from an internal image request.
   *
   * @param request The internal image request containing the prompt and dimensions.
   * @param provider The target provider name for provider-specific option mapping.
   * @return A configured {@link ImagePrompt} with provider-specific options.
   */
  static ImagePrompt toImagePrompt(InternalImageRequest request, String provider) {
    ImageOptions springOptions = OptionsMapper.mapImageOptions(request, provider);
    return new ImagePrompt(List.of(new ImageMessage(request.prompt())), springOptions);
  }
}
