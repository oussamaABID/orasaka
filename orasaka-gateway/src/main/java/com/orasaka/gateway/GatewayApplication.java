package com.orasaka.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import graphql.schema.GraphQLScalarType;

@SpringBootApplication(scanBasePackages = "com.orasaka")
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        GraphQLScalarType mapScalar = GraphQLScalarType.newScalar()
                .name("Map")
                .description("Map scalar")
                .coercing(new graphql.schema.Coercing<Object, Object>() {
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
