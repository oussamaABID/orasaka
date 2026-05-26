package com.orasaka.gateway.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.application.engine.GraphEngine;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigTest {

  private IdentityService identityService;
  private GraphQlCorsProperties graphQlCorsProperties;
  private RateLimitFilter rateLimitFilter;
  private GraphEngine graphEngine;
  private SecurityConfig securityConfig;

  @BeforeEach
  void setUp() {
    identityService = mock(IdentityService.class);
    graphQlCorsProperties = mock(GraphQlCorsProperties.class);
    rateLimitFilter = mock(RateLimitFilter.class);
    graphEngine = mock(GraphEngine.class);
    securityConfig =
        new SecurityConfig(
            identityService, graphQlCorsProperties, Optional.of(rateLimitFilter), graphEngine);
  }

  @Test
  void testAuthenticationManagerSuccess() {
    String token = "valid-token";
    User user =
        new User(
            java.util.UUID.randomUUID(),
            "testuser",
            "test@example.class",
            true,
            Set.of("ROLE_USER"),
            java.util.Map.of());
    when(identityService.getUser(token)).thenReturn(user);

    AuthenticationManager manager = securityConfig.authenticationManager();
    Authentication authInput = new BearerTokenAuthenticationToken(token);
    Authentication authResult = manager.authenticate(authInput);

    assertNotNull(authResult);
    assertTrue(authResult instanceof UsernamePasswordAuthenticationToken);
    assertEquals(user, authResult.getPrincipal());
    assertEquals(token, authResult.getCredentials());
    assertEquals(1, authResult.getAuthorities().size());
    assertEquals("ROLE_USER", authResult.getAuthorities().iterator().next().getAuthority());
  }

  @Test
  void testAuthenticationManagerUserDisabled() {
    String token = "valid-token";
    User user =
        new User(
            java.util.UUID.randomUUID(),
            "testuser",
            "test@example.class",
            false,
            Set.of("ROLE_USER"),
            java.util.Map.of());
    when(identityService.getUser(token)).thenReturn(user);

    AuthenticationManager manager = securityConfig.authenticationManager();
    Authentication authInput = new BearerTokenAuthenticationToken(token);

    assertThrows(BadCredentialsException.class, () -> manager.authenticate(authInput));
  }

  @Test
  void testAuthenticationManagerExceptionThrown() {
    String token = "invalid-token";
    when(identityService.getUser(token)).thenThrow(new RuntimeException("Database down"));

    AuthenticationManager manager = securityConfig.authenticationManager();
    Authentication authInput = new BearerTokenAuthenticationToken(token);

    assertThrows(BadCredentialsException.class, () -> manager.authenticate(authInput));
  }

  @Test
  void testAuthenticationManagerUnsupportedToken() {
    AuthenticationManager manager = securityConfig.authenticationManager();
    Authentication authInput = mock(Authentication.class);

    assertThrows(ProviderNotFoundException.class, () -> manager.authenticate(authInput));
  }

  @Test
  void testCorsConfigurationSourceNullOrigins() {
    when(graphQlCorsProperties.allowedOrigins()).thenReturn(null);

    assertThrows(IllegalStateException.class, () -> securityConfig.corsConfigurationSource());
  }

  @Test
  void testCorsConfigurationSourceEmptyOrigins() {
    when(graphQlCorsProperties.allowedOrigins()).thenReturn(List.of());

    assertThrows(IllegalStateException.class, () -> securityConfig.corsConfigurationSource());
  }

  @Test
  void testCorsConfigurationSourceValid() {
    when(graphQlCorsProperties.allowedOrigins()).thenReturn(List.of("http://localhost:3000"));
    when(graphQlCorsProperties.allowedMethods()).thenReturn(List.of("GET", "POST"));
    when(graphQlCorsProperties.allowedHeaders()).thenReturn(List.of("*"));
    when(graphQlCorsProperties.allowCredentials()).thenReturn(true);

    var source = securityConfig.corsConfigurationSource();
    assertNotNull(source);

    // Test that resolving CORS configuration matches expectations
    var request = new org.springframework.mock.web.MockHttpServletRequest();
    request.setRequestURI("/api/v1/something");
    CorsConfiguration config = source.getCorsConfiguration(request);
    assertNotNull(config);
    assertEquals(List.of("http://localhost:3000"), config.getAllowedOrigins());
    assertEquals(List.of("GET", "POST"), config.getAllowedMethods());
    assertEquals(List.of("*"), config.getAllowedHeaders());
    assertTrue(config.getAllowCredentials());
  }
}
