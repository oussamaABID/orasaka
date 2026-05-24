package com.orasaka.core.support;

/**
 * Unified image request record for multi-modal generation.
 *
 * @param prompt Descriptive text for the image to generate.
 * @param width Desired width in pixels (optional).
 * @param height Desired height in pixels (optional).
 * @param options Provider-specific generation options.
 * @param context The execution context carrying user preferences (e.g., aspect ratios).
 */
public record ImageRequest(
    String prompt, Integer width, Integer height, Options options, Context context) {}
