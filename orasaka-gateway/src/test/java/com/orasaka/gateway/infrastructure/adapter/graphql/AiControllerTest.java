package com.orasaka.gateway.infrastructure.adapter.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.application.engine.GraphEngine;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.gateway.GatewayApplication;
import com.orasaka.gateway.infrastructure.config.MapScalarConfig;
import com.orasaka.gateway.infrastructure.config.SecurityExecutorConfiguration;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import com.orasaka.identity.infrastructure.config.IdentityInfrastructureProperties;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static com.orasaka.test.TestConstants.*;

/** Unit tests verifying AiController capability routing and request processing. */
@GraphQlTest(AiController.class)
@ContextConfiguration(classes = GatewayApplication.class)
@Import({MapScalarConfig.class, SecurityExecutorConfiguration.class})
class AiControllerTest {

  @Autowired private GraphQlTester graphQlTester;

  @MockitoBean private CatalogModelManager catalogModelManager;

  @MockitoBean private AiClient aiClient;

  @MockitoBean private IdentityService identityService;

  @MockitoBean private IdentityInfrastructureProperties identityProperties;

  @MockitoBean private GraphEngine graphEngine;

  @MockitoBean private UserProfileProvider userProfileProvider;

  /** Set up testing environment and mock user session context. */
  @BeforeEach
  void setUp() {
    User mockUser =
        new User(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            "test-user",
            "test-user@orasaka.com",
            true,
            Set.of("ROLE_USER"),
            Map.of("tts-voice", "alloy"),
            List.of());
    when(identityService.getUser("550e8400-e29b-41d4-a716-446655440000")).thenReturn(mockUser);

    var authorities = mockUser.authorities().stream().map(SimpleGrantedAuthority::new).toList();
    var authToken = new UsernamePasswordAuthenticationToken(mockUser, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(authToken);
  }

  /** Verify that user details and chat properties are routed cleanly. */
  @Test
  void shouldPropagateContextOnChat() {
    ChatResponse mockResponse = new ChatResponse("Hello from AI", SESSION_1, Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(mockResponse);

    String document =
        """
            mutation {
                chat(prompt: "Hello", conversationId: "session-123") {
                    content
                    conversationId
                }
            }
        """;

    graphQlTester
        .document(document)
        .execute()
        .path("chat.content")
        .entity(String.class)
        .isEqualTo("Hello from AI")
        .path("chat.conversationId")
        .entity(String.class)
        .isEqualTo(SESSION_1);

    ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
    verify(aiClient).chat(requestCaptor.capture());

    ChatRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.prompt()).isEqualTo("Hello");
    assertThat(capturedRequest.context()).isNotNull();
    assertThat(capturedRequest.context().userId())
        .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    assertThat(capturedRequest.context().conversationId()).isEqualTo(SESSION_1);
    assertThat(capturedRequest.context().preferences()).containsEntry("tts-voice", "alloy");
  }
}
