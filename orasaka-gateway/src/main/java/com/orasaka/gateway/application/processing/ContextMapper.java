package com.orasaka.gateway.application.processing;

import com.orasaka.core.domain.model.Authority;
import com.orasaka.core.domain.model.Context;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserProfile;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Package-private mapping utility for assembling a {@link Context} from an authenticated {@link
 * User} and an optional {@link UserProfile}.
 *
 * <p>Extracts all repetitive inline {@code buildContext} blocks from individual REST and GraphQL
 * controllers, reducing each call site to a single readable invocation (ERR-114). Placed in the
 * {@code adapter} parent package so all adapter sub-packages ({@code rest}, {@code graphql}, {@code
 * amqp}) share this single canonical mapping without duplication.
 */
final class ContextMapper {

  private ContextMapper() {}

  /**
   * Assembles a {@link Context} from the authenticated user, enriched with profile preferences when
   * a {@link UserProfileProvider} is available.
   *
   * @param user The authenticated domain user.
   * @param conversationId Optional conversation ID (may be {@code null}).
   * @param userProfileProvider The profile provider (may be {@code null}).
   * @return Fully assembled {@link Context} ready for the AI pipeline.
   */
  public static Context buildContext(
      User user, String conversationId, UserProfileProvider userProfileProvider) {
    String userId = user.id().toString();
    Set<Authority> authorities =
        user.authorities().stream().map(Authority::new).collect(Collectors.toSet());
    Map<String, Object> preferences = new HashMap<>(user.preferences());
    if (userProfileProvider != null) {
      UserProfile profile = userProfileProvider.getProfile(userId);
      if (profile != null) {
        preferences.put("theme", profile.theme());
        preferences.put("voiceModel", profile.voiceModel());
        preferences.put("primaryIndustry", profile.primaryIndustry());
        preferences.put("primary_industry", profile.primaryIndustry());
        preferences.put("aiBehavior", profile.aiBehavior());
        preferences.put("ai_behavior", profile.aiBehavior());
        if (profile.rawPreferences() != null) {
          preferences.putAll(profile.rawPreferences());
        }
      }
    }
    return new Context(userId, conversationId, preferences, authorities);
  }
}
