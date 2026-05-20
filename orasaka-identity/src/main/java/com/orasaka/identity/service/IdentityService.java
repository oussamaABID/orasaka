package com.orasaka.identity.service;

import com.orasaka.identity.domain.Role;
import com.orasaka.identity.domain.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for managing user identity and preferences.
 */
@Service
public class IdentityService {

    /**
     * Resolves a user profile by ID.
     * In a real system, this would fetch from a database.
     */
    public User getUser(String userId) {
        // Return a mock user with some preferences
        return new User(
            UUID.fromString(userId),
            "vance_cyber",
            new Role.Admin(),
            Map.of(
                "tts-voice", "alloy",
                "image-aspect-ratio", "16:9",
                "chat-temperature", 0.7
            )
        );
    }
}
