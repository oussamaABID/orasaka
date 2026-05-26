package com.orasaka.identity.infrastructure.support;

/** Exception thrown when cryptographic operations fail or security guardrails are violated. */
public class SecurityGuardrailException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message Detailed message.
   * @param cause Underlying exception trigger.
   */
  public SecurityGuardrailException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified message.
   *
   * @param message Detailed message.
   */
  public SecurityGuardrailException(String message) {
    super(message);
  }
}
