package com.orasaka.core.ingest.video;

/**
 * Port interface for pre-processing video input before cognitive analysis.
 *
 * <p>Implementations extract keyframes and transcripts from raw video bytes for downstream
 * multi-modal model consumption. Activated conditionally via {@code
 * orasaka.engine.video.analysis.enabled=true}.
 */
public interface VideoPreProcessor {

  /**
   * Processes raw video bytes into a structured payload containing extracted keyframes and an audio
   * transcript.
   *
   * @param videoBytes The raw video file content.
   * @return A structured payload ready for multi-modal model consumption.
   */
  ProcessedVideoPayload process(byte[] videoBytes);
}
