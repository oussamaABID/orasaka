package com.orasaka.core.engine.video;

/** Immutable response payload containing generated video bytes and target format. */
public record OrasakaVideoResponse(byte[] videoData, String format // Default to "mp4"
    ) {
  public OrasakaVideoResponse {
    if (videoData == null || videoData.length == 0) {
      throw new IllegalArgumentException("Video data cannot be null or empty");
    }
    format = (format != null && !format.isBlank()) ? format : "mp4";
  }
}
