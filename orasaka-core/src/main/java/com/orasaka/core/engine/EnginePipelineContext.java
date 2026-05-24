package com.orasaka.core.engine;

import org.springframework.ai.chat.prompt.Prompt;

/**
 * Immutable compiled context snapshot produced by the {@link EnginePipelineBridge}.
 *
 * <p>Captures the results of prompt refinement, provider routing, conversation tracking, and the
 * final assembled {@link Prompt} ready for model invocation.
 *
 * @param provider The resolved AI provider name (e.g., {@code "ollama"}).
 * @param conversationId The conversation thread ID extracted from the request context.
 * @param promptText The refined or original prompt text string.
 * @param toPrompt The fully assembled Spring AI {@link Prompt} with messages and options.
 */
record EnginePipelineContext(
    String provider, String conversationId, String promptText, Prompt toPrompt) {}
