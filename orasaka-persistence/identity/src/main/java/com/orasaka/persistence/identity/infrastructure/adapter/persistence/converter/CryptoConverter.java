package com.orasaka.persistence.identity.infrastructure.adapter.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Transparent two-way JPA AttributeConverter that encrypts/decrypts fields written to the database
 * using AES-256.
 *
 * <p>Key and salt are injected via {@link CryptoConfigHolder} which reads typed Spring properties —
 * never hardcoded or via raw {@code System.getenv()}.
 */
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

  private final TextEncryptor encryptor;

  /**
   * Initializes the AES-256 text encryptor with key and salt from the Spring-managed config holder.
   *
   * @throws IllegalStateException if ORASAKA_CRYPTO_KEY or ORASAKA_CRYPTO_SALT is missing
   */
  public CryptoConverter() {
    String key = CryptoConfigHolder.getKey();
    String salt = CryptoConfigHolder.getSalt();
    if (key == null || key.isBlank()) {
      throw new IllegalStateException(
          "ORASAKA_CRYPTO_KEY environment variable must be set for AES-256 encryption");
    }
    if (salt == null || salt.isBlank()) {
      throw new IllegalStateException(
          "ORASAKA_CRYPTO_SALT environment variable must be set (16-char hex)");
    }
    this.encryptor = Encryptors.text(key, salt);
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }
    return encryptor.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    return encryptor.decrypt(dbData);
  }
}
