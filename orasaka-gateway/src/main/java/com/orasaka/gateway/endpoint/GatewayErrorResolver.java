package com.orasaka.gateway.endpoint;

import com.orasaka.identity.exception.InvalidRequestException;
import com.orasaka.identity.exception.UserAlreadyExistsException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Unified gateway exception resolver handling REST and GraphQL endpoints. */
@Component
@RestControllerAdvice(basePackages = "com.orasaka.gateway.endpoint")
public class GatewayErrorResolver extends DataFetcherExceptionResolverAdapter {

  private static final Logger logger = LoggerFactory.getLogger(GatewayErrorResolver.class);

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    return switch (ex) {
      case UserAlreadyExistsException e -> {
        logger.warn("GraphQL UserAlreadyExistsException intercepted: {}", e.getMessage());
        yield GraphqlErrorBuilder.newError(env)
            .errorType(ErrorType.BAD_REQUEST)
            .message(e.getMessage())
            .build();
      }
      case InvalidRequestException e -> {
        logger.warn("GraphQL InvalidRequestException intercepted: {}", e.getMessage());
        yield GraphqlErrorBuilder.newError(env)
            .errorType(ErrorType.BAD_REQUEST)
            .message(e.getMessage())
            .build();
      }
      default -> null;
    };
  }

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
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
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
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
  }
}
