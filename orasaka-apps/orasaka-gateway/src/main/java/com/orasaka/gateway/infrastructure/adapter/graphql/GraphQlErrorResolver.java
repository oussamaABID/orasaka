package com.orasaka.gateway.infrastructure.adapter.graphql;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import com.orasaka.identity.infrastructure.support.UserAlreadyExistsException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/** Dedicated GraphQL exception resolver for the gateway endpoint queries and mutations. */
@Component
public class GraphQlErrorResolver extends DataFetcherExceptionResolverAdapter {

  private static final Logger GQL_ERROR_LOGGER =
      LoggerFactory.getLogger(GraphQlErrorResolver.class);

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    return switch (ex) {
      case UserAlreadyExistsException e -> {
        GQL_ERROR_LOGGER.warn("GraphQL UserAlreadyExistsException intercepted: {}", e.getMessage());
        yield GraphqlErrorBuilder.newError(env)
            .errorType(ErrorType.BAD_REQUEST)
            .message(e.getMessage())
            .build();
      }
      case InvalidRequestException e -> {
        GQL_ERROR_LOGGER.warn("GraphQL InvalidRequestException intercepted: {}", e.getMessage());
        yield GraphqlErrorBuilder.newError(env)
            .errorType(ErrorType.BAD_REQUEST)
            .message(e.getMessage())
            .build();
      }
      default -> null;
    };
  }
}
