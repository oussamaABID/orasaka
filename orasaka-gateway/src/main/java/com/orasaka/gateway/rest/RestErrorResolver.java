package com.orasaka.gateway.rest;

import com.orasaka.identity.exception.InvalidRequestException;
import com.orasaka.identity.exception.UserAlreadyExistsException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller advice that intercepts domain exceptions thrown by REST controllers and translates
 * them into semantic HTTP responses.
 */
@RestControllerAdvice(basePackages = "com.orasaka.gateway.rest")
public class RestErrorResolver {

  private static final Logger logger = LoggerFactory.getLogger(RestErrorResolver.class);

  /**
   * Handles user already exists business validation exceptions, translating them to 409 Conflict.
   *
   * @param ex The exception instance.
   * @return A ResponseEntity with 409 Conflict status and the error message.
   */
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Map<String, String>> handleUserAlreadyExists(
      UserAlreadyExistsException ex) {
    logger.warn("REST UserAlreadyExistsException intercepted: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
  }

  /**
   * Handles invalid input parameters, translating them to 400 Bad Request.
   *
   * @param ex The exception instance.
   * @return A ResponseEntity with 400 Bad Request status and the error message.
   */
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<Map<String, String>> handleInvalidRequest(InvalidRequestException ex) {
    logger.warn("REST InvalidRequestException intercepted: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
  }
}
