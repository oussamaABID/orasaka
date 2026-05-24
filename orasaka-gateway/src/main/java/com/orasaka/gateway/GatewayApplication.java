package com.orasaka.gateway;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Orasaka Gateway Backend-for-Frontend (BFF) application.
 *
 * <p>Configures and runs the Spring Boot web application, setting up base package scanning to
 * automatically discover gateway routes, controllers, and services under the {@code com.orasaka}
 * hierarchy.
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication(scanBasePackages = "com.orasaka")
@ConfigurationPropertiesScan("com.orasaka")
@EnableScheduling
public class GatewayApplication {

  /**
   * Bootstraps and launches the Gateway application using {@link SpringApplication}.
   *
   * @param args Command line arguments passed to the application during startup.
   */
  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

  /**
   * Registers and configures a custom GraphQL scalar type for generic {@code Map} objects.
   *
   * <p>This custom scalar is essential for passing arbitrary preferences and metadata JSON blocks
   * dynamically across the GraphQL schema without mapping them to rigid static Java structures.
   *
   * @return A {@link RuntimeWiringConfigurer} instance containing the custom {@code Map} scalar
   *     wiring.
   * @see graphql.schema.GraphQLScalarType
   * @see org.springframework.graphql.execution.RuntimeWiringConfigurer
   */
  @Bean
  public RuntimeWiringConfigurer runtimeWiringConfigurer() {
    GraphQLScalarType mapScalar =
        GraphQLScalarType.newScalar()
            .name("Map")
            .description("Map scalar")
            .coercing(
                new Coercing<Object, Object>() {
                  @Override
                  public Object serialize(
                      Object dataFetcherResult,
                      graphql.GraphQLContext graphQLContext,
                      Locale locale) {
                    return dataFetcherResult;
                  }

                  @Override
                  public Object parseValue(
                      Object input, graphql.GraphQLContext graphQLContext, Locale locale) {
                    return input;
                  }

                  @Override
                  public Object parseLiteral(
                      graphql.language.Value<?> input,
                      graphql.execution.CoercedVariables variables,
                      graphql.GraphQLContext graphQLContext,
                      Locale locale) {
                    return parseLiteralValue(input);
                  }

                  private Object parseLiteralValue(graphql.language.Value<?> value) {
                    if (value instanceof graphql.language.StringValue stringValue) {
                      return stringValue.getValue();
                    }
                    if (value instanceof graphql.language.BooleanValue booleanValue) {
                      return booleanValue.isValue();
                    }
                    if (value instanceof graphql.language.IntValue intValue) {
                      return intValue.getValue();
                    }
                    if (value instanceof graphql.language.FloatValue floatValue) {
                      return floatValue.getValue();
                    }
                    if (value instanceof graphql.language.NullValue) {
                      return null;
                    }
                    if (value instanceof graphql.language.ArrayValue arrayValue) {
                      return arrayValue.getValues().stream().map(this::parseLiteralValue).toList();
                    }
                    if (value instanceof graphql.language.ObjectValue objectValue) {
                      Map<String, Object> map = new HashMap<>();
                      objectValue
                          .getObjectFields()
                          .forEach(
                              field ->
                                  map.put(field.getName(), parseLiteralValue(field.getValue())));
                      return map;
                    }
                    return value != null ? value.toString() : null;
                  }
                })
            .build();

    return wiringBuilder -> wiringBuilder.scalar(mapScalar);
  }
}
