package com.orasaka.gateway.config;

import com.orasaka.identity.domain.User;
import com.orasaka.identity.service.IdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * HTTP authentication filter that extracts Bearer tokens or query parameter tokens, resolves the
 * associated {@link User} from the {@link IdentityService}, and sets the Spring Security context.
 *
 * <p>Supports a development bypass mode for local testing when {@code
 * orasaka.gateway.security.dev-bypass-enabled} is {@code true}.
 *
 * <p>Only UUID-formatted tokens are accepted to prevent injection attacks.
 *
 * @see IdentityService
 * @see GatewayProperties.SecurityProperties
 * @since 1.0.0
 */
class SecurityFilter extends OncePerRequestFilter {

  public static final String DEV_ADMIN_USER_ID = "550e8400-e29b-41d4-a716-446655440001";
  private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);
  private final IdentityService identityService;
  private final GatewayProperties.SecurityProperties securityProperties;

  /**
   * @param identityService Service for resolving users from the identity layer.
   * @param securityProperties Security configuration properties (dev bypass, etc.).
   */
  public SecurityFilter(
      IdentityService identityService, GatewayProperties.SecurityProperties securityProperties) {
    this.identityService = identityService;
    this.securityProperties = securityProperties;
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    extractUserId(request)
        .flatMap(this::safeGetUser)
        .filter(User::enabled)
        .ifPresent(
            user -> {
              var authorities =
                  user.authorities().stream().map(SimpleGrantedAuthority::new).toList();
              var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
              SecurityContextHolder.getContext().setAuthentication(authToken);
            });

    filterChain.doFilter(request, response);
  }

  /**
   * Extracts the user ID from the Authorization header (Bearer token) or the {@code token} query
   * parameter.
   *
   * @param request The incoming HTTP request.
   * @return An {@link Optional} containing the validated token string.
   */
  private Optional<String> extractUserId(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader("Authorization"))
        .filter(header -> header.startsWith("Bearer "))
        .map(header -> header.substring(7).trim())
        .filter(token -> !token.isEmpty())
        .or(
            () ->
                Optional.ofNullable(request.getParameter("token"))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty()))
        .filter(token -> isValidTokenLayout(token) || isDevBypass(token));
  }

  /**
   * Validates that the token is a valid UUID format.
   *
   * @param token The token string to validate.
   * @return {@code true} if the token is a valid UUID.
   */
  private boolean isValidTokenLayout(String token) {
    if (token == null) {
      return false;
    }
    try {
      UUID.fromString(token);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Checks if the token matches the configured dev bypass ID.
   *
   * @param token The token to check.
   * @return {@code true} if dev bypass is enabled and the token matches.
   */
  private boolean isDevBypass(String token) {
    if (securityProperties.devBypassEnabled()
        && securityProperties.devBypassId() != null
        && !securityProperties.devBypassId().isBlank()) {
      return securityProperties.devBypassId().equals(token);
    }
    return false;
  }

  /**
   * Safely resolves a {@link User} from the identity service.
   *
   * @param userId The user ID string (UUID format).
   * @return An {@link Optional} containing the user, or empty on failure.
   */
  private Optional<User> safeGetUser(String userId) {
    try {
      return Optional.ofNullable(identityService.getUser(userId));
    } catch (Exception e) {
      logger.error("Failed to resolve user context from identity layer for userId: {}", userId, e);
      return Optional.empty();
    }
  }
}
