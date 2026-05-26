package com.orasaka.identity.infrastructure.support;

/**
 * Thrown when a required configuration resource (schema, template) cannot be resolved at runtime.
 *
 */
public class ConfigurationException extends RuntimeException {

  /** Constructs exception with a descriptive message. */
  public ConfigurationException(String message) {
    super(message);
  }
}
