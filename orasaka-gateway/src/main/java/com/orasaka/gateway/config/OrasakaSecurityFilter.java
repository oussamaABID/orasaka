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

public class OrasakaSecurityFilter extends OncePerRequestFilter {

  public static final String DEV_ADMIN_USER_ID = "550e8400-e29b-41d4-a716-446655440001";
  private static final Logger logger = LoggerFactory.getLogger(OrasakaSecurityFilter.class);
  private final IdentityService identityService;
  private final GatewayProperties.SecurityProperties securityProperties;

  public OrasakaSecurityFilter(
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

  private boolean isDevBypass(String token) {
    if (securityProperties.devBypassEnabled()
        && securityProperties.devBypassId() != null
        && !securityProperties.devBypassId().isBlank()) {
      return securityProperties.devBypassId().equals(token);
    }
    return false;
  }

  private Optional<User> safeGetUser(String userId) {
    try {
      return Optional.ofNullable(identityService.getUser(userId));
    } catch (Exception e) {
      logger.error("Failed to resolve user context from identity layer for userId: {}", userId, e);
      return Optional.empty();
    }
  }
}
