package com.orasaka.gateway.infrastructure.config;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Custom coercing implementation for the GraphQL Map scalar, utilizing Java 21+ pattern matching
 * switches.
 */
final class MapScalarCoercing implements Coercing<Object, Object> {

  @Override
  public Object serialize(Object dataFetcherResult, GraphQLContext graphQLContext, Locale locale) {
    return dataFetcherResult;
  }

  @Override
  public Object parseValue(Object input, GraphQLContext graphQLContext, Locale locale) {
    return input;
  }

  @Override
  public Object parseLiteral(
      Value<?> input, CoercedVariables variables, GraphQLContext graphQLContext, Locale locale) {
    return parseLiteralValue(input);
  }

  private Object parseLiteralValue(Value<?> value) {
    return switch (value) {
      case StringValue stringValue -> stringValue.getValue();
      case BooleanValue booleanValue -> booleanValue.isValue();
      case IntValue intValue -> intValue.getValue();
      case FloatValue floatValue -> floatValue.getValue();
      case NullValue nullValue -> null;
      case ArrayValue arrayValue ->
          arrayValue.getValues().stream().map(this::parseLiteralValue).toList();
      case ObjectValue objectValue -> {
        Map<String, Object> map = new HashMap<>();
        objectValue
            .getObjectFields()
            .forEach(field -> map.put(field.getName(), parseLiteralValue(field.getValue())));
        yield map;
      }
      case null -> null;
      default -> value.toString();
    };
  }
}
