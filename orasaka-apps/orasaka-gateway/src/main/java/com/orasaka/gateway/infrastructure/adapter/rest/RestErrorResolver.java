package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.gateway.infrastructure.support.SystemOverloadedException;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import com.orasaka.identity.infrastructure.support.UserAlreadyExistsException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Dedicated REST exception advice for the gateway endpoints. */
@RestControllerAdvice(basePackages = "com.orasaka.gateway.infrastructure.adapter.rest")
public class RestErrorResolver {

  private static final Logger logger = LoggerFactory.getLogger(RestErrorResolver.class);
  private static final String ERROR_KEY = "error";

  /**
   * Handles user conflicts.
   *
   * @param ex The conflict exception.
   * @return Error response with 409 Conflict.
   */
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Map<String, String>> handleUserAlreadyExists(
      UserAlreadyExistsException ex) {
    logger.warn("REST UserAlreadyExistsException intercepted: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(ERROR_KEY, ex.getMessage()));
  }

  /**
   * Handles invalid request arguments.
   *
   * @param ex The bad request exception.
   * @return Error response with 400 Bad Request.
   */
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<Map<String, String>> handleInvalidRequest(InvalidRequestException ex) {
    logger.warn("REST InvalidRequestException intercepted: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(ERROR_KEY, ex.getMessage()));
  }

  /**
   * Handles broker overload/failures.
   *
   * @param ex The system overloaded exception.
   * @return Error response with 429 Too Many Requests.
   */
  @ExceptionHandler(SystemOverloadedException.class)
  public ResponseEntity<Map<String, String>> handleSystemOverloaded(SystemOverloadedException ex) {
    logger.warn("REST SystemOverloadedException intercepted: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(Map.of(ERROR_KEY, ex.getMessage()));
  }
}
