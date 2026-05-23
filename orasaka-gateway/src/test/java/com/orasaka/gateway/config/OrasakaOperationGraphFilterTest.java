package com.orasaka.gateway.config;

import static org.mockito.Mockito.*;

import com.orasaka.core.engine.*;
import com.orasaka.core.engine.NodeState.Active;
import com.orasaka.core.engine.NodeState.Invisible;
import com.orasaka.core.engine.NodeState.Locked;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

/** Unit tests verifying OrasakaOperationGraphFilter requests blocking. */
class OrasakaOperationGraphFilterTest {

  private OrasakaGraphEngine graphEngine;
  private OrasakaOperationGraphFilter filter;

  @BeforeEach
  void setUp() {
    graphEngine = mock(OrasakaGraphEngine.class);
    filter = new OrasakaOperationGraphFilter(graphEngine);
  }

  @Test
  void testFilterAllowsActiveOperation() throws Exception {
    OrasakaOperationGraph graph =
        new OrasakaOperationGraph(
            List.of(
                new OperationNode(
                    "orasaka.core.chat.text",
                    "Text Chat",
                    "chat",
                    "CONTEXT_MENU_PLUS",
                    new Active(),
                    new TargetExecutionUri("/api/v1/chat/stream", "POST", "{}"))));
    when(graphEngine.compileGraph()).thenReturn(graph);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/api/v1/chat/stream");
    when(request.getMethod()).thenReturn("POST");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  void testFilterBlocksLockedOperation() throws Exception {
    OrasakaOperationGraph graph =
        new OrasakaOperationGraph(
            List.of(
                new OperationNode(
                    "orasaka.core.chat.text",
                    "Text Chat",
                    "chat",
                    "CONTEXT_MENU_PLUS",
                    new Locked("Maintenance", java.time.LocalDateTime.now()),
                    new TargetExecutionUri("/api/v1/chat/stream", "POST", "{}"))));
    when(graphEngine.compileGraph()).thenReturn(graph);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/api/v1/chat/stream");
    when(request.getMethod()).thenReturn("POST");

    StringWriter stringWriter = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

    filter.doFilterInternal(request, response, filterChain);

    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void testFilterInterceptsGraphQLQuery() throws Exception {
    OrasakaOperationGraph graph =
        new OrasakaOperationGraph(
            List.of(
                new OperationNode(
                    "orasaka.core.chat.image",
                    "Generate Image",
                    "image",
                    "CONTEXT_MENU_PLUS",
                    new Invisible(),
                    new TargetExecutionUri("/graphql", "POST", "{}"))));
    when(graphEngine.compileGraph()).thenReturn(graph);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/graphql");
    when(request.getMethod()).thenReturn("POST");

    String requestBody = "{\"query\":\"mutation { image(prompt: \\\"hello\\\") }\"}";
    byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
    ServletInputStream inputStream =
        new DelegatingServletInputStream(new ByteArrayInputStream(bodyBytes));
    when(request.getInputStream()).thenReturn(inputStream);

    StringWriter stringWriter = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

    filter.doFilterInternal(request, response, filterChain);

    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }
}
