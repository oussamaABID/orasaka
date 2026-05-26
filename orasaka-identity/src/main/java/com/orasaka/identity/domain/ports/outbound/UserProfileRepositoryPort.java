package com.orasaka.identity.domain.ports.outbound;

import com.orasaka.identity.domain.model.UserProfile;
import java.util.Optional;

/** Outbound port defining user profile persistence operations. */
public interface UserProfileRepositoryPort {

  /**
   * Resolves the onboarding profile for a given user ID.
   *
   * @param userId The unique user identifier.
   * @return An Optional containing the UserProfile.
   */
  Optional<UserProfile> findByUserId(String userId);
}
