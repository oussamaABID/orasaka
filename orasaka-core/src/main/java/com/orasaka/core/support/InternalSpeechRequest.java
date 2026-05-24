package com.orasaka.core.support;

/**
 * Internal engine-level text-to-speech request record.
 *
 * <p>Distinct from the public-facing {@link SpeechRequest}, this is consumed directly by {@link
 * com.orasaka.core.engine.AbstractEngine#generateSpeech}.
 *
 * @param text The text content to synthesize into audio (required, non-blank).
 * @param options Provider-specific speech generation options (nullable).
 * @param context Execution context with user preferences (e.g., TTS voice, speed).
 * @see com.orasaka.core.engine.MediaPayloadHandler
 * @since 1.0.0
 */
public record InternalSpeechRequest(String text, Options options, Context context) {
  /** Compact constructor — validates the text is non-null and non-blank. */
  public InternalSpeechRequest {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Text cannot be empty");
    }
  }
}
