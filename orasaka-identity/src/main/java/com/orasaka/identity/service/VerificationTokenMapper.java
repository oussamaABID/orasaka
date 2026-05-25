package com.orasaka.identity.service;

import com.orasaka.identity.entity.VerificationTokenEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Package-private static mapper isolating {@link VerificationTokenEntity} construction boilerplate
 * from business logic services.
 *
 * <p>This is a pure, side-effect-free transformation utility. It must never be made public — the
 * mapping responsibility is confined to the identity service package.
 */
final class VerificationTokenMapper {

  private VerificationTokenMapper() {}

  /**
   * Creates a new email verification token entity ready for persistence.
   *
   * @param userId The user identifier the token belongs to.
   * @param tokenHash The pre-computed SHA-256 hash of the plaintext token.
   * @param expiry The expiration timestamp for this token.
   * @return A fully populated, unsaved entity.
   */
  static VerificationTokenEntity toEntity(String userId, String tokenHash, Instant expiry) {
    VerificationTokenEntity entity = new VerificationTokenEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setUserId(userId);
    entity.setTokenType("EMAIL_VERIFICATION");
    entity.setTokenHash(tokenHash);
    entity.setExpiryTimestamp(expiry);
    entity.setUsed(false);
    entity.setCreatedAt(Instant.now());
    return entity;
  }
}
