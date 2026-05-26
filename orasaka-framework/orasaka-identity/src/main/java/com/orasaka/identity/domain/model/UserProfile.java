package com.orasaka.identity.domain.model;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable domain representation of a user profile.
 *
 * <p>Carries preferences like theme, voice model, primary industry domain, and custom AI behavior
 * system prompt constraints.
 */
public record UserProfile(
    String userId,
    String theme,
    String voiceModel,
    String primaryIndustry,
    String aiBehavior,
    Map<String, Object> rawPreferences) {

  /** Compact constructor enforcing null checks and defensive copy of raw preferences. */
  public UserProfile {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(theme, "theme must not be null");
    Objects.requireNonNull(voiceModel, "voiceModel must not be null");
    Objects.requireNonNull(primaryIndustry, "primaryIndustry must not be null");
    rawPreferences = (rawPreferences == null) ? Map.of() : Map.copyOf(rawPreferences);
  }
}
