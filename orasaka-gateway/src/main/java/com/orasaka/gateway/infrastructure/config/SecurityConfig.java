package com.orasaka.gateway.infrastructure.config;

import com.orasaka.core.application.engine.GraphEngine;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless Spring Security configuration for the {@code orasaka-gateway} module.
 *
 * <p>Defines the security filter chain, CORS policy, and request authorization rules. All session
 * management is disabled ({@link SessionCreationPolicy#STATELESS}); every incoming request is
 * authenticated on-the-fly via official Spring Security OAuth2 resource server filters.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final String ADMIN = "ROLE_ADMIN";
  private static final String USER = "ROLE_USER";

  private final IdentityService identityService;
  private final GraphQlCorsProperties graphQlCorsProperties;
  private final Optional<RateLimitFilter> rateLimitFilter;
  private final GraphEngine graphEngine;

  public SecurityConfig(
      IdentityService identityService,
      GraphQlCorsProperties graphQlCorsProperties,
      Optional<RateLimitFilter> rateLimitFilter,
      GraphEngine graphEngine) {
    this.identityService = identityService;
    this.graphQlCorsProperties = graphQlCorsProperties;
    this.rateLimitFilter = rateLimitFilter;
    this.graphEngine = graphEngine;
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return authentication -> {
      if (authentication instanceof BearerTokenAuthenticationToken bearerToken) {
        String token = bearerToken.getToken();
        User user;
        try {
          user = identityService.getUser(token);
        } catch (RuntimeException ex) {
          throw new BadCredentialsException("Invalid or inactive session token");
        }
        if (!user.enabled()) {
          throw new BadCredentialsException("Invalid or inactive session token");
        }
        var authorities = user.authorities().stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(user, token, authorities);
      }
      throw new ProviderNotFoundException("Unsupported authentication token type");
    };
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
    var resolver = new DefaultBearerTokenResolver();
    resolver.setAllowUriQueryParameter(true);

    // Justified: Stateless OAuth2 Resource Server — no cookies, no session state.
    // CSRF protection is not applicable for token-based (Bearer) authentication.
    // See AGENTS.md §7.1: "CSRF disabled for stateless API."
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/register")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/verify")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/oauth")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/forgot")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/reset")
                    .permitAll()
                    .requestMatchers("/api/v1/operations/graph")
                    .hasAnyAuthority(ADMIN, USER)
                    .requestMatchers("/api/v1/bootstrap/features")
                    .hasAnyAuthority(ADMIN, USER)
                    .requestMatchers("/uploads/**")
                    .hasAnyAuthority(ADMIN, USER)
                    .requestMatchers("/api/v1/jobs/*/progress")
                    .permitAll()
                    .requestMatchers("/graphiql")
                    .permitAll()
                    .requestMatchers("/graphql")
                    .authenticated()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/api/v1/chat/stream/**")
                    .hasAnyAuthority(ADMIN, USER)
                    .requestMatchers("/api/v1/user/credentials/**")
                    .hasAnyAuthority(ADMIN, USER)
                    .requestMatchers("/api/v1/jobs/**")
                    .hasAnyAuthority(ADMIN, USER)
                    .requestMatchers("/api/v1/models")
                    .hasAnyAuthority(ADMIN, USER)
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenResolver(resolver)
                    .opaqueToken(opaque -> opaque.authenticationManager(authenticationManager)));

    http.addFilterAfter(
        new OperationGraphFilter(graphEngine), BearerTokenAuthenticationFilter.class);
    rateLimitFilter.ifPresent(
        filter -> http.addFilterAfter(filter, BearerTokenAuthenticationFilter.class));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    if (graphQlCorsProperties.allowedOrigins() == null
        || graphQlCorsProperties.allowedOrigins().isEmpty()) {
      throw new IllegalStateException(
          "CORS allowed origins are unresolved. Configure spring.graphql.cors.allowed-origins.");
    }
    configuration.setAllowedOrigins(graphQlCorsProperties.allowedOrigins());
    configuration.setAllowedMethods(graphQlCorsProperties.allowedMethods());
    configuration.setAllowedHeaders(graphQlCorsProperties.allowedHeaders());
    configuration.setAllowCredentials(graphQlCorsProperties.allowCredentials());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
