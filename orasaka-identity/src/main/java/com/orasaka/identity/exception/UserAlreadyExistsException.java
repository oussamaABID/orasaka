package com.orasaka.identity.exception;

/**
 * Exception thrown when a user registration fails because the requested email is already
 * registered.
 */
public class UserAlreadyExistsException extends RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message The detail message.
   */
  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
