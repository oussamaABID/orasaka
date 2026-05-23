package com.orasaka.gateway.rest;

/**
 * Immutable request payload for the {@code POST /api/v1/auth/oauth} endpoint.
 *
 * <p>Carries social profile attributes mapping user identities resolved from NextAuth social
 * providers.
 *
 * @param email The user's registered social email address.
 * @param username The user's screen name or display handle.
 * @see com.orasaka.gateway.rest.ChatStreamController#oauthLogin(OAuthRequest)
 */
public record OAuthRequest(String email, String username) {}
