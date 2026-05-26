package com.orasaka.core.domain.model.image;

import java.util.Arrays;
import java.util.Objects;

/**
 * Unified image response record.
 *
 * @param imageData The raw binary data of the generated image (may be null if URL is used).
 * @param url The public URL of the generated image.
 * @param format The image file format (e.g., "png", "jpg").
 */
public record ImageResponse(byte[] imageData, String url, String format) {

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ImageResponse(byte[] otherData, String otherUrl, String otherFormat)))
      return false;
    return Arrays.equals(imageData, otherData)
        && Objects.equals(url, otherUrl)
        && Objects.equals(format, otherFormat);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(imageData);
    result = 31 * result + Objects.hashCode(url);
    result = 31 * result + Objects.hashCode(format);
    return result;
  }

  @Override
  public String toString() {
    int dataLen = imageData != null ? imageData.length : 0;
    return "ImageResponse[imageData=" + dataLen + " bytes, url=" + url + ", format=" + format + "]";
  }
}
