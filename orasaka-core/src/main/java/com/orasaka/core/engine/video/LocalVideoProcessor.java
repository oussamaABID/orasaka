package com.orasaka.core.engine.video;

import com.orasaka.core.engine.CoreProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local adapter for video pre-processing. Extracts keyframes from raw video bytes based on
 * configurable intervals and limits.
 *
 * <p>Bean registration is delegated to the gateway layer via explicit {@code @Configuration}.
 * This class remains web-agnostic per Section 1.A of AGENTS.md.
 */
public class LocalVideoProcessor implements VideoPreProcessor {

  private static final Logger logger = LoggerFactory.getLogger(LocalVideoProcessor.class);

  private final int maxKeyframes;
  private final int frameIntervalSec;

  LocalVideoProcessor(CoreProperties properties) {
    Objects.requireNonNull(properties, "CoreProperties must not be null");
    var analysis = resolveAnalysisConfig(properties);
    this.maxKeyframes = analysis.maxKeyframes() > 0 ? analysis.maxKeyframes() : 8;
    this.frameIntervalSec = analysis.frameIntervalSec() > 0 ? analysis.frameIntervalSec() : 5;
    logger.info(
        "LocalVideoProcessor initialized: maxKeyframes={}, frameIntervalSec={}",
        this.maxKeyframes,
        this.frameIntervalSec);
  }

  @Override
  public ProcessedVideoPayload process(byte[] videoBytes) {
    Objects.requireNonNull(videoBytes, "Video bytes must not be null");
    List<byte[]> keyframes = extractKeyframes(videoBytes);
    String transcript = extractTranscript(videoBytes);
    return new ProcessedVideoPayload(transcript, keyframes);
  }

  private List<byte[]> extractKeyframes(byte[] videoBytes) {
    // Stub: partition raw bytes into segments as keyframe placeholders.
    // Real implementation would use FFmpeg or a native frame extractor.
    int segmentSize = Math.max(1, videoBytes.length / maxKeyframes);
    List<byte[]> frames = new ArrayList<>();
    for (int i = 0; i < videoBytes.length && frames.size() < maxKeyframes; i += segmentSize) {
      int end = Math.min(i + segmentSize, videoBytes.length);
      frames.add(Arrays.copyOfRange(videoBytes, i, end));
    }
    logger.debug("Extracted {} keyframes (interval={}s)", frames.size(), frameIntervalSec);
    return frames;
  }

  private String extractTranscript(byte[] videoBytes) {
    // Stub: Whisper integration placeholder.
    logger.debug("Audio transcription stub — returning empty transcript.");
    return "";
  }

  private static CoreProperties.VideoAnalysisConfig resolveAnalysisConfig(
      CoreProperties properties) {
    return (properties.video() != null && properties.video().analysis() != null)
        ? properties.video().analysis()
        : new CoreProperties.VideoAnalysisConfig(false, 8, 5);
  }
}
