package com.orasaka.core.ingest.audio;

import java.util.Objects;

/**
 * Immutable payload carrying the transcribed text from a processed audio source.
 *
 * <p>Compact constructor enforces null-safety per ADR-007.
 *
 * @param transcript The transcribed audio track (empty string if unavailable).
 */
public record ProcessedAudioPayload(String transcript) {

  /** Compact constructor enforcing null-safety. */
  public ProcessedAudioPayload {
    transcript = Objects.requireNonNullElse(transcript, "");
  }
}
