package com.orasaka.core.application.processing;

import java.util.List;
import java.util.Objects;

/**
 * Immutable payload carrying extracted keyframes and audio transcript from a processed video.
 *
 * <p>Compact constructor enforces defensive copying and null-safety per ADR-007.
 *
 * @param audioTranscript The transcribed audio track (empty string if unavailable).
 * @param keyframes Extracted keyframe images as raw byte arrays.
 */
public record ProcessedVideoPayload(String audioTranscript, List<byte[]> keyframes) {

  /** Compact constructor enforcing immutability and null-safety. */
  public ProcessedVideoPayload {
    audioTranscript = Objects.requireNonNullElse(audioTranscript, "");
    keyframes = (keyframes == null) ? List.of() : List.copyOf(keyframes);
  }
}
