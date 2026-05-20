package com.orasaka.core.context;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable context for Orasaka AI requests.
 * Carries multi-session and multi-modal preferences.
 */
public record OrasakaContext(
    String userId,
    String conversationId,
    Map<String, Object> preferences
) {
    public OrasakaContext {
        // Defensive copy of preferences
        preferences = (preferences != null) ? Collections.unmodifiableMap(Map.copyOf(preferences)) : Collections.emptyMap();
    }
}
