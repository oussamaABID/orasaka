package com.orasaka.core.support;

/**
 * Internal engine-level image generation response record.
 *
 * <p>Contains the raw image bytes, an optional URL, and the format identifier.
 *
 * @param imageData The raw binary image data (nullable if URL is used instead).
 * @param url The image URL or RFC 2397 Data URL.
 * @param format The image format identifier (e.g., {@code "png"}, {@code "jpg"}).
 * @see InternalImageRequest
 * @since 1.0.0
 */
public record InternalImageResponse(byte[] imageData, String url, String format) {}
