package com.orasaka.gateway.graphql;

import com.orasaka.core.client.OrasakaAiClient;
import com.orasaka.core.model.OrasakaChatRequest;
import com.orasaka.core.model.OrasakaChatResponse;
import com.orasaka.identity.domain.Role;
import com.orasaka.identity.domain.User;
import com.orasaka.identity.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@GraphQlTest(AiController.class)
public class AiControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private OrasakaAiClient aiClient;

    @MockBean
    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        User mockUser = new User(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), "test-user", new Role.Operator(), Map.of("tts-voice", "alloy"));
        when(identityService.getUser("550e8400-e29b-41d4-a716-446655440000")).thenReturn(mockUser);
    }

    @Test
    void shouldPropagateContextOnChat() {
        // Given
        OrasakaChatResponse mockResponse = new OrasakaChatResponse("Hello from AI", "session-123", Map.of());
        when(aiClient.chat(any(OrasakaChatRequest.class))).thenReturn(mockResponse);

        String document = """
            mutation {
                chat(prompt: "Hello", conversationId: "session-123") {
                    content
                    conversationId
                }
            }
        """;

        // When
        graphQlTester.document(document)
                .execute()
                .path("chat.content").entity(String.class).isEqualTo("Hello from AI")
                .path("chat.conversationId").entity(String.class).isEqualTo("session-123");

        // Then
        ArgumentCaptor<OrasakaChatRequest> requestCaptor = ArgumentCaptor.forClass(OrasakaChatRequest.class);
        verify(aiClient).chat(requestCaptor.capture());

        OrasakaChatRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.prompt()).isEqualTo("Hello");
        assertThat(capturedRequest.context()).isNotNull();
        assertThat(capturedRequest.context().userId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(capturedRequest.context().conversationId()).isEqualTo("session-123");
        assertThat(capturedRequest.context().preferences()).containsEntry("tts-voice", "alloy");
    }
}
