package com.orasaka.persistence.identity.infrastructure.adapter.persistence.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring-managed holder that bridges typed configuration properties to the JPA {@link
 * CryptoConverter}.
 *
 * <p>JPA {@code @Converter} classes require a no-arg constructor and cannot receive constructor DI.
 * This component loads crypto secrets from Spring properties and exposes them via static accessors
 * so the converter can read them at instantiation time — eliminating raw {@code System.getenv()}
 * calls.
 *
 * @since 1.1.0
 */
@Component
class CryptoConfigHolder {

  private static String key;
  private static String salt;

  @SuppressWarnings("java:S3010") // Static assignment is intentional — JPA converter bridge pattern
  CryptoConfigHolder(
      @Value("${orasaka.crypto.key:}") String cryptoKey,
      @Value("${orasaka.crypto.salt:}") String cryptoSalt) {
    CryptoConfigHolder.key = cryptoKey;
    CryptoConfigHolder.salt = cryptoSalt;
  }

  static String getKey() {
    if (key != null && !key.isBlank()) {
      return key;
    }
    return System.getenv("ORASAKA_CRYPTO_KEY");
  }

  static String getSalt() {
    if (salt != null && !salt.isBlank()) {
      return salt;
    }
    return System.getenv("ORASAKA_CRYPTO_SALT");
  }
}
