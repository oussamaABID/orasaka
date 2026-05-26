package com.orasaka.gateway.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GraphQlCorsProperties}. */
class GraphQlCorsPropertiesTest {

  @Test
  @DisplayName("GraphQlCorsProperties stores CORS configurations")
  void graphqlCorsProperties() {
    var cors =
        new GraphQlCorsProperties(
            List.of("http://localhost:3000"),
            List.of("GET", "POST"),
            List.of("Authorization"),
            true);
    assertEquals(List.of("http://localhost:3000"), cors.allowedOrigins());
    assertEquals(List.of("GET", "POST"), cors.allowedMethods());
    assertEquals(List.of("Authorization"), cors.allowedHeaders());
    assertTrue(cors.allowCredentials());
  }
}
