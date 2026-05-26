package com.orasaka.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

  @Test
  void validConstruction_setsAllFields() {
    Instant expires = Instant.now().plusSeconds(900);
    var token = new PasswordResetToken("id-1", "user@example.com", "hash123", expires);
    assertEquals("id-1", token.id());
    assertEquals("user@example.com", token.email());
    assertEquals("hash123", token.tokenHash());
    assertEquals(expires, token.expiresAt());
  }

  @Test
  void nullId_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new PasswordResetToken(null, "email", "hash", now));
  }

  @Test
  void nullEmail_throws() {
    var now = Instant.now();
    assertThrows(NullPointerException.class, () -> new PasswordResetToken("id", null, "hash", now));
  }

  @Test
  void nullTokenHash_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new PasswordResetToken("id", "email", null, now));
  }

  @Test
  void nullExpiresAt_throws() {
    assertThrows(
        NullPointerException.class, () -> new PasswordResetToken("id", "email", "hash", null));
  }
}
