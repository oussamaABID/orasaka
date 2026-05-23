package com.orasaka.gateway.graphql;

import com.orasaka.identity.exception.InvalidRequestException;
import com.orasaka.identity.exception.UserAlreadyExistsException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * GraphQL exception resolver that intercepts domain/business exceptions and translates them into
 * clean GraphQL errors.
 */
@Component
public class GraphqlErrorResolver extends DataFetcherExceptionResolverAdapter {

  private static final Logger logger = LoggerFactory.getLogger(GraphqlErrorResolver.class);

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    if (ex instanceof UserAlreadyExistsException) {
      logger.warn("GraphQL UserAlreadyExistsException intercepted: {}", ex.getMessage());
      return GraphqlErrorBuilder.newError(env)
          .errorType(ErrorType.BAD_REQUEST)
          .message(ex.getMessage())
          .build();
    } else if (ex instanceof InvalidRequestException) {
      logger.warn("GraphQL InvalidRequestException intercepted: {}", ex.getMessage());
      return GraphqlErrorBuilder.newError(env)
          .errorType(ErrorType.BAD_REQUEST)
          .message(ex.getMessage())
          .build();
    }
    return null;
  }
}
