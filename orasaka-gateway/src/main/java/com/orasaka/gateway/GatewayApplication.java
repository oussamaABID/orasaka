package com.orasaka.gateway;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
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
 * @see com.orasaka.gateway.controller.ChatStreamController
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
                  public Object serialize(Object dataFetcherResult) {
                    return dataFetcherResult;
                  }

                  @Override
                  public Object parseValue(Object input) {
                    return input;
                  }

                  @Override
                  public Object parseLiteral(Object input) {
                    return input;
                  }
                })
            .build();

    return wiringBuilder -> wiringBuilder.scalar(mapScalar);
  }
}
