package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.orasaka.gateway.infrastructure.support.SystemOverloadedException;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import com.orasaka.identity.infrastructure.support.UserAlreadyExistsException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RestErrorResolverTest {

  private final RestErrorResolver resolver = new RestErrorResolver();

  @Test
  @DisplayName("handleUserAlreadyExists returns 409 Conflict")
  void handleUserAlreadyExists() {
    UserAlreadyExistsException ex = new UserAlreadyExistsException("User exists");
    ResponseEntity<Map<String, String>> response = resolver.handleUserAlreadyExists(ex);

    assertNotNull(response);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("User exists", response.getBody().get("error"));
  }

  @Test
  @DisplayName("handleInvalidRequest returns 400 Bad Request")
  void handleInvalidRequest() {
    InvalidRequestException ex = new InvalidRequestException("Invalid prompt");
    ResponseEntity<Map<String, String>> response = resolver.handleInvalidRequest(ex);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Invalid prompt", response.getBody().get("error"));
  }

  @Test
  @DisplayName("handleSystemOverloaded returns 429 Too Many Requests")
  void handleSystemOverloaded() {
    SystemOverloadedException ex = new SystemOverloadedException("System is overloaded");
    ResponseEntity<Map<String, String>> response = resolver.handleSystemOverloaded(ex);

    assertNotNull(response);
    assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    assertEquals("System is overloaded", response.getBody().get("error"));
  }
}
