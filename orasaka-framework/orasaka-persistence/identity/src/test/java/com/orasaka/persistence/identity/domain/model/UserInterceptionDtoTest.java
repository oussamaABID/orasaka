package com.orasaka.persistence.identity.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserInterceptionDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    Instant now = Instant.now();
    var dto = new UserInterceptionDto("user-1", "REFINER", true, now);
    assertEquals("user-1", dto.userId());
    assertEquals("REFINER", dto.interceptionType());
    assertTrue(dto.active());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void nullUserId_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new UserInterceptionDto(null, "type", true, now));
  }

  @Test
  void nullInterceptionType_throws() {
    var now = Instant.now();
    assertThrows(
        NullPointerException.class, () -> new UserInterceptionDto("user", null, true, now));
  }

  @Test
  void nullActive_defaultsToTrue() {
    var dto = new UserInterceptionDto("user", "ROUTER", null, Instant.now());
    assertTrue(dto.active());
  }

  @Test
  void falseActive_preserved() {
    var dto = new UserInterceptionDto("user", "REFINER", false, Instant.now());
    assertFalse(dto.active());
  }
}
