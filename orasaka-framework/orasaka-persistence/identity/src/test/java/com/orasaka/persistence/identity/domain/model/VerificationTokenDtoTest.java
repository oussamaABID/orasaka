package com.orasaka.persistence.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class VerificationTokenDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    Instant expires = Instant.now().plusSeconds(3600);
    Instant created = Instant.now();
    var dto =
        new VerificationTokenDto(
            "token-1", "user-1", "EMAIL_VERIFY", "hash", expires, false, created);
    assertEquals("token-1", dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("EMAIL_VERIFY", dto.tokenType());
    assertEquals("hash", dto.tokenHash());
    assertEquals(expires, dto.expiryTimestamp());
    assertFalse(dto.used());
    assertEquals(created, dto.createdAt());
  }

  @Test
  void nullId_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class,
        () -> new VerificationTokenDto(null, "user", "type", "hash", now, false, now));
  }

  @Test
  void nullUserId_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class,
        () -> new VerificationTokenDto("id", null, "type", "hash", now, false, now));
  }

  @Test
  void nullTokenType_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class,
        () -> new VerificationTokenDto("id", "user", null, "hash", now, false, now));
  }

  @Test
  void nullTokenHash_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class,
        () -> new VerificationTokenDto("id", "user", "type", null, now, false, now));
  }

  @Test
  void nullExpiryTimestamp_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class,
        () -> new VerificationTokenDto("id", "user", "type", "hash", null, false, now));
  }

  @Test
  void nullUsed_defaultsToFalse() {
    var dto =
        new VerificationTokenDto("id", "user", "type", "hash", Instant.now(), null, Instant.now());
    assertFalse(dto.used());
  }
}
