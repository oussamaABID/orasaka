package com.orasaka.identity.infrastructure.adapter.crypto;

import com.orasaka.identity.domain.ports.outbound.CryptographyPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Concrete outbound infrastructure adapter implementing {@link CryptographyPort}. Wraps {@link
 * BCryptPasswordEncoder} and standard SHA-256 digesting.
 */
@Component
class SpringSecurityCryptographyAdapter implements CryptographyPort {

  private final BCryptPasswordEncoder passwordEncoder;

  SpringSecurityCryptographyAdapter(BCryptPasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to hash token: SHA-256 digest unavailable", e);
    }
  }

  @Override
  public String encodePassword(String password) {
    return passwordEncoder.encode(password);
  }

  @Override
  public boolean matchesPassword(String rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
  }
}
