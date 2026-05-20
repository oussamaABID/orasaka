package com.orasaka.core.model;

import com.orasaka.core.context.OrasakaContext;

/**
 * Unified speech request record for Text-To-Speech generation.
 * 
 * @param text The text content to convert to speech.
 * @param options Provider-specific speech options.
 * @param context The execution context carrying user preferences (e.g., voice models, speed).
 */
public record OrasakaSpeechRequest(
    String text,
    OrasakaOptions options,
    OrasakaContext context
) {}
