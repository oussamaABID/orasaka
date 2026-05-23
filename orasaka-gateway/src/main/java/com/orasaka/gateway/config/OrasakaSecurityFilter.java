package com.orasaka.gateway.config;

import com.orasaka.identity.domain.User;
import com.orasaka.identity.service.IdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
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
    String authHeader = request.getHeader("Authorization");
    String userId = null;

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      userId = authHeader.substring(7).trim();
    } else {
      String tokenParam = request.getParameter("token");
      if (tokenParam != null && !tokenParam.isBlank()) {
        userId = tokenParam.trim();
      }
    }

    User user = null;
    if (userId != null) {
      try {
        user = identityService.getUser(userId);
      } catch (Exception e) {
        // Ignore and proceed to fallback
      }
    }

    if (user != null && user.enabled()) {
      var authorities =
          user.authorities().stream()
              .map(auth -> new SimpleGrantedAuthority(auth.name()))
              .collect(Collectors.toList());
      var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    filterChain.doFilter(request, response);
  }
}
