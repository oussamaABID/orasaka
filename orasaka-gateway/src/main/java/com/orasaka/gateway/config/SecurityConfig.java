package com.orasaka.gateway.config;

import com.orasaka.core.engine.OrasakaGraphEngine;
import com.orasaka.identity.service.IdentityService;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless Spring Security configuration for the {@code orasaka-gateway} module.
 *
 * <p>Defines the security filter chain, CORS policy, and request authorization rules. All session
 * management is disabled ({@link SessionCreationPolicy#STATELESS}); every incoming request is
 * authenticated on-the-fly by {@link OrasakaSecurityFilter}.
 *
 * <p>CORS allowed origins are environment-driven via {@code orasaka.gateway.cors.allowed-origins}
 * in {@code application.yml}, ensuring zero hardcoded infrastructure values in compiled artifacts.
 *
 * @see OrasakaSecurityFilter
 * @see com.orasaka.identity.service.IdentityService
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final IdentityService identityService;
  private final GatewayProperties gatewayProperties;
  private final Optional<RateLimitFilter> rateLimitFilter;
  private final OrasakaGraphEngine graphEngine;

  /**
   * Constructs the security configuration with its dependencies.
   *
   * @param identityService The service used by {@link OrasakaSecurityFilter} to resolve
   *     authenticated users from the Bearer token header.
   * @param gatewayProperties Environment-driven gateway properties config mapping.
   * @param rateLimitFilter The optional rate limiting filter to register.
   * @param graphEngine The short-circuit graph compilation engine.
   */
  public SecurityConfig(
      IdentityService identityService,
      GatewayProperties gatewayProperties,
      Optional<RateLimitFilter> rateLimitFilter,
      OrasakaGraphEngine graphEngine) {
    this.identityService = identityService;
    this.gatewayProperties = gatewayProperties;
    this.rateLimitFilter = rateLimitFilter;
    this.graphEngine = graphEngine;
  }

  /**
   * Declares the primary Spring Security filter chain.
   *
   * <p>Security rules applied (in order of precedence):
   *
   * <ol>
   *   <li>CSRF protection disabled — stateless API does not use cookies.
   *   <li>CORS configured from {@link #corsConfigurationSource()}.
   *   <li>All {@code OPTIONS} preflight requests permitted unconditionally.
   *   <li>{@code /api/v1/auth/login} and GraphQL explorer endpoints are public.
   *   <li>SSE streaming endpoint requires {@code ROLE_ADMIN} or {@code ROLE_USER} authority.
   *   <li>All other requests require authentication.
   *   <li>{@link OrasakaSecurityFilter} runs before the standard form-login filter.
   * </ol>
   *
   * @param http The {@link HttpSecurity} builder provided by Spring Security.
   * @return The fully configured {@link SecurityFilterChain}.
   * @throws Exception If any security configuration step fails during initialization.
   * @see OrasakaSecurityFilter
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    HttpSecurity chain =
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth ->
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/register")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/verify")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/oauth")
                        .permitAll()
                        .requestMatchers("/api/v1/operations/graph")
                        .permitAll()
                        .requestMatchers("/graphiql")
                        .permitAll()
                        .requestMatchers("/graphql")
                        .authenticated()
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers("/api/v1/chat/stream/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER")
                        .anyRequest()
                        .authenticated())
            .addFilterBefore(
                new OrasakaSecurityFilter(identityService),
                UsernamePasswordAuthenticationFilter.class);

    chain.addFilterAfter(new OrasakaOperationGraphFilter(graphEngine), OrasakaSecurityFilter.class);
    rateLimitFilter.ifPresent(filter -> chain.addFilterAfter(filter, OrasakaSecurityFilter.class));

    return chain.build();
  }

  /**
   * Produces the environment-driven CORS configuration source.
   *
   * <p>Allowed origins are resolved from the {@link GatewayProperties} configuration record,
   * enabling multi-environment deployments without recompilation.
   *
   * @return A {@link CorsConfigurationSource} applied globally to all request paths.
   * @see org.springframework.web.cors.CorsConfiguration
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    String allowedOrigins =
        (gatewayProperties.cors() != null && gatewayProperties.cors().allowedOrigins() != null)
            ? gatewayProperties.cors().allowedOrigins()
            : "http://localhost:3000";
    configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-Requested-With", "X-Orasaka-Context"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
