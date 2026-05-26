package com.orasaka.gateway.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.orasaka.identity.domain.model.RateLimitInfo;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.RateLimitProvider;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests verifying security filter chain rate limit processing. */
class RateLimitFilterTest {

  private ProxyManager<String> proxyManager;
  private RateLimitProvider rateLimitProvider;
  private RateLimitProperties properties;
  private RateLimitFilter filter;
  private SecurityContext originalSecurityContext;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    proxyManager = mock(ProxyManager.class);
    rateLimitProvider = mock(RateLimitProvider.class);
    properties = new RateLimitProperties(true, "free");
    filter = new RateLimitFilter(proxyManager, rateLimitProvider, properties);
    originalSecurityContext = SecurityContextHolder.getContext();
    SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.setContext(originalSecurityContext);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldAllowRequestWhenTokensAvailableForUser() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    User user =
        new User(
            userId,
            "test-user",
            "test@orasaka.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of(),
            "premium");
    var authToken = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authToken);

    // Mock DB query for "premium" tier
    RateLimitInfo tierDto = new RateLimitInfo("premium", 100, 5);
    when(rateLimitProvider.getRateLimit("premium")).thenReturn(Optional.of(tierDto));

    // Mock Bucket4j bucket & consumption probe
    RemoteBucketBuilder<String> bucketBuilder = mock(RemoteBucketBuilder.class);
    BucketProxy bucket = mock(BucketProxy.class);
    ConsumptionProbe probe = mock(ConsumptionProbe.class);

    when(proxyManager.builder()).thenReturn(bucketBuilder);
    when(bucketBuilder.build(eq(userId.toString() + ":premium"), any(Supplier.class)))
        .thenReturn(bucket);
    when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);
    when(probe.getRemainingTokens()).thenReturn(99L);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    verify(response).setHeader("X-RateLimit-Limit", "100");
    verify(response).setHeader("X-RateLimit-Remaining", "99");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldDenyRequestWhenTokensExhaustedForUser() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    User user =
        new User(
            userId,
            "test-user",
            "test@orasaka.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of(),
            "free");
    var authToken = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authToken);

    // Mock DB query for "free" tier
    RateLimitInfo tierDto = new RateLimitInfo("free", 10, 1);
    when(rateLimitProvider.getRateLimit("free")).thenReturn(Optional.of(tierDto));

    // Mock Bucket4j bucket & consumption probe
    RemoteBucketBuilder<String> bucketBuilder = mock(RemoteBucketBuilder.class);
    BucketProxy bucket = mock(BucketProxy.class);
    ConsumptionProbe probe = mock(ConsumptionProbe.class);

    when(proxyManager.builder()).thenReturn(bucketBuilder);
    when(bucketBuilder.build(eq(userId.toString() + ":free"), any(Supplier.class)))
        .thenReturn(bucket);
    when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(false);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    StringWriter out = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(out));

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    verify(response).setStatus(429);
    verify(response).setHeader("X-RateLimit-Limit", "10");
    verify(response).setHeader("X-RateLimit-Remaining", "0");
    verify(response).setHeader("Content-Type", "application/json");
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFallbackToIpWhenAnonymous() throws Exception {
    // Given
    SecurityContextHolder.getContext().setAuthentication(null);

    // Mock DB query for "free" default tier
    RateLimitInfo tierDto = new RateLimitInfo("free", 10, 1);
    when(rateLimitProvider.getRateLimit("free")).thenReturn(Optional.of(tierDto));

    RemoteBucketBuilder<String> bucketBuilder = mock(RemoteBucketBuilder.class);
    BucketProxy bucket = mock(BucketProxy.class);
    ConsumptionProbe probe = mock(ConsumptionProbe.class);

    when(proxyManager.builder()).thenReturn(bucketBuilder);
    when(bucketBuilder.build(eq("192.168.1.50:free"), any(Supplier.class))).thenReturn(bucket);
    when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);
    when(probe.getRemainingTokens()).thenReturn(9L);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("192.168.1.50");
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    verify(response).setHeader("X-RateLimit-Limit", "10");
    verify(response).setHeader("X-RateLimit-Remaining", "9");
    verify(filterChain).doFilter(request, response);
  }
}
