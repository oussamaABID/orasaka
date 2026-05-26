package com.orasaka.gateway.application.processing;

import com.orasaka.core.domain.model.Authority;
import com.orasaka.core.domain.model.Context;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
   * a {@link UserProfile} is available.
   *
   * @param user The authenticated domain user (non-null).
   * @param conversationId Optional conversation ID (may be {@code null}).
   * @param sessionId The explicit inbound request session tracking token (non-null) to maintain
   *     cross-service traceability.
   * @param profile The enriched profile preferences (may be {@code null}).
   * @return Fully assembled {@link Context} ready for the AI pipeline.
   */
  public static Context buildContext(
      User user, String conversationId, UUID sessionId, UserProfile profile) {

    Set<Authority> authorities =
        user.authorities().stream().map(Authority::new).collect(Collectors.toSet());

    // Ingestion des préférences de base de l'entité User
    Map<String, Object> preferences = new HashMap<>(user.preferences());

    // Transitive payload: request-bound sessionId passed from interceptor for cross-service
    // traceability
    preferences.put("sessionId", sessionId.toString());

    if (profile != null) {
      // Construction sécurisée du dictionnaire composé sans duplication de casing
      Map<String, Object> profileContext = new HashMap<>();
      if (profile.theme() != null) profileContext.put("theme", profile.theme());
      if (profile.voiceModel() != null) profileContext.put("voiceModel", profile.voiceModel());
      if (profile.primaryIndustry() != null)
        profileContext.put("primaryIndustry", profile.primaryIndustry());
      if (profile.aiBehavior() != null) profileContext.put("aiBehavior", profile.aiBehavior());

      // Transitive payload: consolidated userProfileContext compound map (sealed and immutable)
      preferences.put("userProfileContext", Map.copyOf(profileContext));

      // Inclusion des métadonnées dynamiques floues si elles existent
      if (profile.rawPreferences() != null) {
        preferences.putAll(profile.rawPreferences());
      }
    }

    return new Context(
        user.id().toString(),
        conversationId != null ? conversationId : "none",
        Map.copyOf(preferences),
        authorities);
  }
}
