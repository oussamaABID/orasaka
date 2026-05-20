package com.orasaka.identity.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable User representation using Java 21 Records.
 * Includes user-specific preferences for multi-modal AI.
 */
public record User(UUID id, String username, Role role, Map<String, Object> preferences) {
    public User {
        if (id == null) id = UUID.randomUUID();
        if (preferences == null) preferences = Map.of();
    }
}
