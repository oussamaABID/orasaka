package com.orasaka.identity.event;

import com.orasaka.identity.domain.User;

/**
 * Event published when a new user registers in the Orasaka identity module.
 *
 * @param user The registered user domain object.
 * @param plaintextToken The raw plaintext verification token, if verification is enabled; {@code
 *     null} otherwise.
 */
public record UserRegisteredEvent(User user, String plaintextToken) {}
