package com.orasaka.core.support;

/**
 * Unified image response record.
 *
 * @param imageData The raw binary data of the generated image (may be null if URL is used).
 * @param url The public URL of the generated image.
 * @param format The image file format (e.g., "png", "jpg").
 */
public record ImageResponse(byte[] imageData, String url, String format) {}
