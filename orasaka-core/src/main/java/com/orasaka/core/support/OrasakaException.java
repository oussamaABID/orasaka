package com.orasaka.core.support;

/**
 * Base exception for the Orasaka CORS module. Decouples the client from underlying Spring AI
 * exceptions.
 */
public class OrasakaException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new exception with the specified message.
   *
   * @param message The detail message.
   */
  public OrasakaException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with message and cause.
   *
   * @param message The detail message.
   * @param cause The underlying cause.
   */
  public OrasakaException(String message, Throwable cause) {
    super(message, cause);
  }
}
