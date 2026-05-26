package com.orasaka.identity.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.domain.model.PasswordResetToken;
import com.orasaka.persistence.identity.domain.model.PasswordResetTokenDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PasswordResetTokenMapperTest {

  @Test
  void toDto_mapsAllFields() {
    Instant exp = Instant.now().plusSeconds(3600);
    var domain = new PasswordResetToken("id-1", "john@test.com", "hash", exp);
    PasswordResetTokenDto dto = PasswordResetTokenMapper.toDto(domain);
    assertEquals("id-1", dto.id());
    assertEquals("john@test.com", dto.email());
    assertEquals("hash", dto.tokenHash());
    assertEquals(exp, dto.expiresAt());
  }

  @Test
  void toDomain_mapsAllFields() {
    Instant exp = Instant.now().plusSeconds(3600);
    var dto = new PasswordResetTokenDto("id-1", "john@test.com", "hash", exp, null);
    PasswordResetToken domain = PasswordResetTokenMapper.toDomain(dto);
    assertEquals("id-1", domain.id());
    assertEquals("john@test.com", domain.email());
    assertEquals("hash", domain.tokenHash());
    assertEquals(exp, domain.expiresAt());
  }

  @Test
  void roundTrip_preservesData() {
    Instant exp = Instant.now().plusSeconds(3600);
    var original = new PasswordResetToken("id-1", "john@test.com", "hash", exp);
    PasswordResetToken roundTripped =
        PasswordResetTokenMapper.toDomain(PasswordResetTokenMapper.toDto(original));
    assertEquals(original, roundTripped);
  }
}
