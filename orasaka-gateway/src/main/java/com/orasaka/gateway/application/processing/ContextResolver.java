package com.orasaka.gateway.application.processing;

import com.orasaka.core.domain.model.Context;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;

/**
 * Public facade to resolve orchestration context from the authenticated user and their profile
 * preferences. Delegates internally to the package-private {@link ContextMapper} to maintain strict
 * encapsulation (ERR-110).
 */
public final class ContextResolver {

  private ContextResolver() {}

  /**
   * Resolves orchestration context for a given user.
   *
   * @param user The authenticated domain user.
   * @param conversationId Optional conversation ID (may be {@code null}).
   * @param userProfileProvider The profile provider (may be {@code null}).
   * @return Fully assembled {@link Context}.
   */
  public static Context resolve(
      User user, String conversationId, UserProfileProvider userProfileProvider) {
    return ContextMapper.buildContext(user, conversationId, userProfileProvider);
  }
}
