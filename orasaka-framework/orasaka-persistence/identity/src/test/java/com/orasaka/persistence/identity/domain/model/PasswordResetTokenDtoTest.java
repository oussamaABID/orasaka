package com.orasaka.persistence.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PasswordResetTokenDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    Instant expires = Instant.now().plusSeconds(3600);
    Instant created = Instant.now();
    var dto = new PasswordResetTokenDto("token-1", "user@example.com", "hash123", expires, created);
    assertEquals("token-1", dto.id());
    assertEquals("user@example.com", dto.email());
    assertEquals("hash123", dto.tokenHash());
    assertEquals(expires, dto.expiresAt());
    assertEquals(created, dto.createdAt());
  }

  @Test
  void nullId_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class,
        () -> new PasswordResetTokenDto(null, "email", "hash", now, now));
  }

  @Test
  void nullEmail_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new PasswordResetTokenDto("id", null, "hash", now, now));
  }

  @Test
  void nullTokenHash_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new PasswordResetTokenDto("id", "email", null, now, now));
  }

  @Test
  void nullExpiresAt_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class,
        () -> new PasswordResetTokenDto("id", "email", "hash", null, now));
  }

  @Test
  void nullCreatedAt_allowed() {
    assertDoesNotThrow(() -> new PasswordResetTokenDto("id", "email", "hash", Instant.now(), null));
  }
}
