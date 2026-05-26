package com.orasaka.gateway.infrastructure.adapter.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.gateway.domain.model.AuthContracts.RegisterResponse;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import com.orasaka.identity.infrastructure.support.UserAlreadyExistsException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityControllerTest {

  private IdentityService identityService;
  private ExecutorService executorService;
  private IdentityController identityController;

  @BeforeEach
  void setUp() {
    identityService = mock(IdentityService.class);
    // Use a direct/same-thread executor for synchronous execution in tests
    executorService = mock(ExecutorService.class);
    doAnswer(
            invocation -> {
              Runnable runnable = invocation.getArgument(0);
              runnable.run();
              return null;
            })
        .when(executorService)
        .execute(any(Runnable.class));

    identityController = new IdentityController(identityService, executorService);
  }

  @Test
  void testAuthorities() {
    User user =
        new User(
            UUID.randomUUID(),
            "testuser",
            "test@test.class",
            true,
            Set.of("ROLE_USER", "ROLE_ADMIN"),
            Map.of());
    List<String> authorities = identityController.authorities(user);

    assertNotNull(authorities);
    assertEquals(2, authorities.size());
    assertTrue(authorities.contains("ROLE_USER"));
    assertTrue(authorities.contains("ROLE_ADMIN"));
  }

  @Test
  void testMe() throws Exception {
    User user =
        new User(
            UUID.randomUUID(), "testuser", "test@test.class", true, Set.of("ROLE_USER"), Map.of());
    CompletableFuture<User> future = identityController.me(user);

    assertNotNull(future);
    User result = future.get();
    assertEquals(user, result);
  }

  @Test
  void testUpdatePreferences() throws Exception {
    UUID userId = UUID.randomUUID();
    User user =
        new User(userId, "testuser", "test@test.class", true, Set.of("ROLE_USER"), Map.of());
    Map<String, Object> prefs = Map.of("theme", "dark");
    User updatedUser =
        new User(userId, "testuser", "test@test.class", true, Set.of("ROLE_USER"), prefs);

    when(identityService.updatePreferences(userId.toString(), prefs)).thenReturn(updatedUser);

    CompletableFuture<User> future = identityController.updatePreferences(prefs, user);

    assertNotNull(future);
    User result = future.get();
    assertEquals(updatedUser, result);
  }

  @Test
  void testRegisterSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    User createdUser =
        new User(userId, "newuser", "new@test.class", true, Set.of("ROLE_USER"), Map.of());
    when(identityService.register("newuser", "new@test.class", "password", "fr"))
        .thenReturn(createdUser);

    CompletableFuture<RegisterResponse> future =
        identityController.register("newuser", "new@test.class", "password", "fr");

    assertNotNull(future);
    RegisterResponse response = future.get();
    assertNotNull(response.user());
    assertNull(response.error());
    assertEquals(userId.toString(), response.user().id());
    assertEquals("newuser", response.user().username());
    assertEquals("new@test.class", response.user().email());
    assertEquals(List.of("ROLE_USER"), response.user().authorities());
  }

  @Test
  void testRegisterAlreadyExists() throws Exception {
    when(identityService.register("newuser", "new@test.class", "password", "fr"))
        .thenThrow(new UserAlreadyExistsException("Email already registered"));

    CompletableFuture<RegisterResponse> future =
        identityController.register("newuser", "new@test.class", "password", "fr");

    assertNotNull(future);
    RegisterResponse response = future.get();
    assertNull(response.user());
    assertEquals("Email already registered", response.error());
  }

  @Test
  void testInterceptionSchema() throws Exception {
    UUID userId = UUID.randomUUID();
    User user =
        new User(userId, "testuser", "test@test.class", true, Set.of("ROLE_USER"), Map.of());
    String schemaJson = "{\"type\":\"object\"}";
    when(identityService.loadInterceptionSchema("schema-1")).thenReturn(schemaJson);

    CompletableFuture<String> future = identityController.interceptionSchema("schema-1", user);

    assertNotNull(future);
    String result = future.get();
    assertEquals(schemaJson, result);
  }

  @Test
  void testResolveInterception() throws Exception {
    UUID userId = UUID.randomUUID();
    User user =
        new User(userId, "testuser", "test@test.class", true, Set.of("ROLE_USER"), Map.of());
    Map<String, Object> responses = Map.of("field", "value");

    CompletableFuture<Boolean> future =
        identityController.resolveInterception("type-A", "schema-1", responses, user);

    assertNotNull(future);
    assertTrue(future.get());
    verify(identityService).resolveInterception(userId, "type-A", "schema-1", responses);
  }
}
