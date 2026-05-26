package com.orasaka.gateway.infrastructure.adapter.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import com.orasaka.identity.infrastructure.support.UserAlreadyExistsException;
import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

class GraphQlErrorResolverTest {

  private DataFetchingEnvironment mockEnvironment() {
    DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
    MergedField mergedField = mock(MergedField.class);
    Field field = mock(Field.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);

    when(env.getMergedField()).thenReturn(mergedField);
    when(mergedField.getSingleField()).thenReturn(field);
    when(env.getField()).thenReturn(field);
    when(field.getSourceLocation()).thenReturn(new SourceLocation(1, 1));
    when(env.getExecutionStepInfo()).thenReturn(stepInfo);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());

    return env;
  }

  @Test
  @DisplayName("resolveToSingleError handles UserAlreadyExistsException correctly")
  void resolvesUserAlreadyExistsException() {
    GraphQlErrorResolver resolver = new GraphQlErrorResolver();
    DataFetchingEnvironment env = mockEnvironment();
    UserAlreadyExistsException ex = new UserAlreadyExistsException("User already exists");

    GraphQLError error = resolver.resolveToSingleError(ex, env);

    assertThat(error).isNotNull();
    assertThat(error.getMessage()).isEqualTo("User already exists");
    assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
  }

  @Test
  @DisplayName("resolveToSingleError handles InvalidRequestException correctly")
  void resolvesInvalidRequestException() {
    GraphQlErrorResolver resolver = new GraphQlErrorResolver();
    DataFetchingEnvironment env = mockEnvironment();
    InvalidRequestException ex = new InvalidRequestException("Invalid request");

    GraphQLError error = resolver.resolveToSingleError(ex, env);

    assertThat(error).isNotNull();
    assertThat(error.getMessage()).isEqualTo("Invalid request");
    assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
  }

  @Test
  @DisplayName("resolveToSingleError returns null for other exceptions")
  void resolvesOtherExceptionsToNull() {
    GraphQlErrorResolver resolver = new GraphQlErrorResolver();
    DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
    RuntimeException ex = new RuntimeException("Other exception");

    GraphQLError error = resolver.resolveToSingleError(ex, env);

    assertThat(error).isNull();
  }
}
