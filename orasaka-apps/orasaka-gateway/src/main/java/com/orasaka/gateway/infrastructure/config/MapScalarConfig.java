package com.orasaka.gateway.infrastructure.config;

import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/** Spring Configuration registering the custom Map scalar for Spring GraphQL. */
@Configuration
public class MapScalarConfig {

  @Bean
  public RuntimeWiringConfigurer runtimeWiringConfigurer() {
    GraphQLScalarType mapScalar =
        GraphQLScalarType.newScalar()
            .name("Map")
            .description("Map scalar representing generic dynamic JSON objects")
            .coercing(new MapScalarCoercing())
            .build();

    return wiringBuilder -> wiringBuilder.scalar(mapScalar);
  }
}
