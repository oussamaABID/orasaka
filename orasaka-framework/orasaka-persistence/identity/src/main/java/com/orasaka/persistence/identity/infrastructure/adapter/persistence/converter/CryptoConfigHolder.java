package com.orasaka.persistence.identity.infrastructure.adapter.persistence.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring-managed holder that bridges typed configuration properties to the JPA {@link
 * CryptoConverter}.
 *
 * <p>JPA {@code @Converter} classes require a no-arg constructor and cannot receive constructor DI.
 * This component loads crypto secrets from Spring properties ({@code crypto.key}, {@code
 * crypto.salt}) and exposes them via static accessors so the converter can read them at
 * instantiation time.
 *
 * <p><b>Race condition note:</b> Hibernate processes {@code @Converter} metadata before the Spring
 * context finishes creating all beans. If the static fields are not yet populated, the getters fall
 * back to {@code System.getenv("CRYPTO_KEY")} / {@code System.getenv("CRYPTO_SALT")} to cover this
 * startup ordering gap.
 *
 * @since 1.1.0
 */
@Component
class CryptoConfigHolder {

  private static String key;
  private static String salt;

  @SuppressWarnings("java:S3010") // Static assignment is intentional — JPA converter bridge pattern
  CryptoConfigHolder(
      @Value("${crypto.key:}") String cryptoKey, @Value("${crypto.salt:}") String cryptoSalt) {
    CryptoConfigHolder.key = cryptoKey;
    CryptoConfigHolder.salt = cryptoSalt;
  }

  static String getKey() {
    if (key != null && !key.isBlank()) {
      return key;
    }
    return System.getenv("CRYPTO_KEY");
  }

  static String getSalt() {
    if (salt != null && !salt.isBlank()) {
      return salt;
    }
    return System.getenv("CRYPTO_SALT");
  }
}
