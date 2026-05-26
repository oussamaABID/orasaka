package com.orasaka.core.domain.model.video;

import java.util.Arrays;
import java.util.Map;

/**
 * Immutable response payload containing generated video bytes, target format and metrics.
 *
 * <p>Overrides {@code equals}, {@code hashCode}, and {@code toString} to properly handle the {@code
 * byte[]} field, which is not structurally compared by default record implementations.
 */
public record VideoResponse(
    byte[] videoData, String format, String outputPath, Map<String, Object> metrics) {
  public VideoResponse {
    if (videoData == null || videoData.length == 0) {
      throw new IllegalArgumentException("Video data cannot be null or empty");
    }
    format = (format != null && !format.isBlank()) ? format : "mp4";
    outputPath = (outputPath != null) ? outputPath : "";
    metrics = (metrics != null) ? Map.copyOf(metrics) : Map.of();
  }

  public VideoResponse(byte[] videoData, String format) {
    this(videoData, format, "", Map.of());
  }

  public VideoResponse(byte[] videoData, String format, Map<String, Object> metrics) {
    this(videoData, format, "", metrics);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o
        instanceof
        VideoResponse(
            byte[] otherData,
            String otherFormat,
            String otherPath,
            Map<String, Object> otherMetrics))) return false;
    return Arrays.equals(videoData, otherData)
        && format.equals(otherFormat)
        && outputPath.equals(otherPath)
        && metrics.equals(otherMetrics);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(videoData);
    result = 31 * result + format.hashCode();
    result = 31 * result + outputPath.hashCode();
    result = 31 * result + metrics.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "VideoResponse[videoData="
        + videoData.length
        + " bytes, format="
        + format
        + ", outputPath="
        + outputPath
        + ", metrics="
        + metrics
        + "]";
  }
}
