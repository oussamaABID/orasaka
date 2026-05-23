package com.orasaka.gateway.config;

import com.orasaka.identity.domain.User;
import com.orasaka.identity.service.IdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stateless security filter that authenticates each HTTP request using a Bearer token.
 *
 * <p>The token payload is expected to be the authenticated user's UUID, consistent with the CLI
 * contract defined in {@code AGENTS.md §6.A}. On every request the filter:
 *
 * <ol>
 *   <li>Extracts the {@code Authorization: Bearer <userId>} header (or {@code ?token=} query
 *       param).
 *   <li>Resolves the corresponding {@link User} via {@link IdentityService}.
 *   <li>Populates the {@link SecurityContextHolder} with the resolved authorities.
 * </ol>
 *
 * <p>This filter is executed once per request (extends {@link OncePerRequestFilter}) and is safe
 * for Virtual Thread execution environments.
 *
 * @see SecurityConfig
 * @see com.orasaka.identity.service.IdentityService
 */
public class OrasakaSecurityFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaSecurityFilter.class);

  private final IdentityService identityService;

  /**
   * Constructs the filter with its identity resolution dependency.
   *
   * @param identityService The service responsible for resolving {@link User} records from the
   *     persistence layer using a user UUID.
   */
  public OrasakaSecurityFilter(IdentityService identityService) {
    this.identityService = identityService;
  }

  /**
   * Controls whether this filter should be applied to asynchronous dispatches.
   *
   * <p>Overridden to return {@code false} to ensure that the security context remains active and is
   * re-evaluated during internal async dispatches (e.g., when forwarding SSE streams via Tomcat).
   *
   * @return Always {@code false} to allow filtering on async dispatches.
   */
  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  /**
   * Performs one-time-per-request authentication resolution.
   *
   * @param request The incoming HTTP request.
   * @param response The HTTP response to write to if authentication is rejected.
   * @param filterChain The remainder of the filter chain to delegate to.
   * @throws ServletException If a servlet-related error occurs during filter processing.
   * @throws IOException If an I/O error occurs while reading request data.
   */
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

  /** Extracts user ID from Authorization header or request token query parameter. */
  private Optional<String> extractUserId(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader("Authorization"))
        .filter(header -> header.startsWith("Bearer "))
        .map(header -> header.substring(7).trim())
        .filter(token -> !token.isEmpty())
        .or(
            () ->
                Optional.ofNullable(request.getParameter("token"))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty()));
  }

  /** Resolves a user profile safely, logging failures if the data layer is unreachable. */
  private Optional<User> safeGetUser(String userId) {
    try {
      return Optional.ofNullable(identityService.getUser(userId));
    } catch (Exception e) {
      logger.error("Failed to resolve user context from identity layer for userId: {}", userId, e);
      return Optional.empty();
    }
  }
}
