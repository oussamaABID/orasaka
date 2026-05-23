package com.orasaka.gateway.rest;

/**
 * Immutable request payload for the {@code POST /api/v1/auth/login} endpoint.
 *
 * <p>Carries plaintext credentials submitted by the client. The password is validated against the
 * BCrypt hash stored in the database by {@link
 * com.orasaka.identity.service.IdentityService#authenticate(String, String)}.
 *
 * @param username The user's login name (case-sensitive).
 * @param password The user's plaintext password to verify against the stored BCrypt hash.
 * @see com.orasaka.gateway.rest.ChatStreamController#login(LoginRequest)
 */
public record LoginRequest(String email, String password) {}
