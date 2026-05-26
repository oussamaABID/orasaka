package com.orasaka.gateway.infrastructure.adapter.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.application.engine.GraphEngine;
import com.orasaka.core.domain.model.NodeState;
import com.orasaka.core.domain.model.OperationGraph;
import com.orasaka.core.domain.model.audio.AudioRequest;
import com.orasaka.core.domain.model.audio.AudioResponse;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.model.image.ImageRequest;
import com.orasaka.core.domain.model.image.ImageResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserProfile;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

  @Mock private AiClient aiClient;
  @Mock private GraphEngine graphEngine;
  @Mock private UserProfileProvider userProfileProvider;
  @Mock private CatalogModelManager catalogModelManager;

  private ExecutorService executorService;
  private AiController controller;
  private User user;
  private UserProfile profile;
  private String userId;

  @BeforeEach
  void setUp() {
    executorService = mock(ExecutorService.class);
    lenient()
        .doAnswer(
            invocation -> {
              Runnable runnable = invocation.getArgument(0);
              runnable.run();
              return null;
            })
        .when(executorService)
        .execute(any(Runnable.class));

    controller =
        new AiController(
            aiClient, graphEngine, userProfileProvider, executorService, catalogModelManager);

    userId = UUID.randomUUID().toString();
    user =
        new User(
            UUID.fromString(userId),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of());
    profile = new UserProfile(userId, "dark", "alloy", "tech", "helpful", Map.of());
  }

  @Test
  void chat_successfulExecution_returnsResponse() throws Exception {
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    ChatResponse expectedResponse = new ChatResponse("AI Response", "conv-1", Map.of());
    when(aiClient.chat(any(ChatRequest.class))).thenReturn(expectedResponse);

    CompletableFuture<ChatResponse> future = controller.chat("Hello AI", "conv-1", user);

    assertNotNull(future);
    ChatResponse actualResponse = future.get();
    assertEquals("AI Response", actualResponse.content());
  }

  @Test
  void chatStream_returnsFluxOfResponses() {
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    ChatResponse responseChunk = new ChatResponse("stream-chunk", "conv-1", null);
    when(aiClient.stream(any(ChatRequest.class))).thenReturn(Flux.just(responseChunk));

    Flux<ChatResponse> flux = controller.chatStream("Stream input", "conv-1", user);

    assertNotNull(flux);
    ChatResponse resolved = flux.blockFirst();
    assertNotNull(resolved);
    assertEquals("stream-chunk", resolved.content());
  }

  @Test
  void image_withDefaultModel_generatesImage() throws Exception {
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    when(catalogModelManager.getDefaultModelByCategory("image")).thenReturn(Optional.empty());

    ImageResponse imgResponse = new ImageResponse(null, "http://image-url", "png");
    when(aiClient.image(any(ImageRequest.class))).thenReturn(imgResponse);

    CompletableFuture<ChatResponse> future = controller.image("Generate something", null, user);

    assertNotNull(future);
    ChatResponse response = future.get();
    assertEquals("http://image-url", response.content());
    assertEquals("png", response.metadata().get("format"));
  }

  @Test
  void image_withModelOverrideAndCatalogDefault_generatesImage() throws Exception {
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);

    ImageResponse imgResponse = new ImageResponse(null, "http://image-url-2", "jpeg");
    when(aiClient.image(any(ImageRequest.class))).thenReturn(imgResponse);

    CompletableFuture<ChatResponse> future =
        controller.image("Generate something", "dalle-3", user);

    assertNotNull(future);
    ChatResponse response = future.get();
    assertEquals("http://image-url-2", response.content());
  }

  @Test
  void speech_withVoiceAndModelDefaults_generatesSpeech() throws Exception {
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    when(catalogModelManager.getDefaultModelByCategory("speech")).thenReturn(Optional.empty());

    AudioResponse audioResponse = new AudioResponse(new byte[] {1, 2, 3}, "mp3");
    when(aiClient.audio(any(AudioRequest.class))).thenReturn(audioResponse);

    CompletableFuture<ChatResponse> future = controller.speech("Speak this", null, null, user);

    assertNotNull(future);
    ChatResponse response = future.get();
    assertTrue(response.content().startsWith("data:audio/mp3;base64,"));
  }

  @Test
  void speech_withCatalogOptions_generatesSpeech() throws Exception {
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);

    AudioResponse audioResponse = new AudioResponse(new byte[] {1, 2, 3}, "mp3");
    when(aiClient.audio(any(AudioRequest.class))).thenReturn(audioResponse);

    CompletableFuture<ChatResponse> future = controller.speech("Speak this", "piper", "ryan", user);

    assertNotNull(future);
    ChatResponse response = future.get();
    assertTrue(response.content().startsWith("data:audio/mp3;base64,"));
  }

  @Test
  void operationGraph_compilesGraph() throws Exception {
    OperationGraph expectedGraph = mock(OperationGraph.class);
    when(graphEngine.compileGraph()).thenReturn(expectedGraph);

    CompletableFuture<OperationGraph> future = controller.operationGraph(user);

    assertNotNull(future);
    assertEquals(expectedGraph, future.get());
  }

  @Test
  void nodeStateType_returnsCorrectString() {
    assertEquals("ACTIVE", controller.nodeStateType(new NodeState.Active()));
    assertEquals(
        "LOCKED",
        controller.nodeStateType(new NodeState.Locked("Maintenance", LocalDateTime.now())));
    assertEquals("INVISIBLE", controller.nodeStateType(new NodeState.Invisible()));
  }

  @Test
  void nodeStateReason_returnsLockedReason() {
    assertNull(controller.nodeStateReason(new NodeState.Active()));
    assertNull(controller.nodeStateReason(new NodeState.Invisible()));
    assertEquals(
        "Maintenance",
        controller.nodeStateReason(new NodeState.Locked("Maintenance", LocalDateTime.now())));
  }

  @Test
  void nodeStateLockedAt_returnsLockedTimestamp() {
    assertNull(controller.nodeStateLockedAt(new NodeState.Invisible()));

    LocalDateTime now = LocalDateTime.now();
    assertEquals(
        now.toString(), controller.nodeStateLockedAt(new NodeState.Locked("Maintenance", now)));
  }
}
