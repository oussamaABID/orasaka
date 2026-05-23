package com.orasaka.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.exception.OrasakaException;
import com.orasaka.core.interceptors.mcp.McpOrchestrator;
import com.orasaka.core.interceptors.mcp.OrasakaMcpInterceptor;
import com.orasaka.core.interceptors.memory.OrasakaMemoryInterceptor;
import com.orasaka.core.interceptors.memory.OrasakaMemoryResolver;
import com.orasaka.core.interceptors.rag.OrasakaKnowledgeService;
import com.orasaka.core.interceptors.rag.OrasakaRagInterceptor;
import com.orasaka.core.interceptors.tool.OrasakaToolInterceptor;
import com.orasaka.core.interceptors.tool.OrasakaToolRegistry;
import com.orasaka.core.model.OrasakaChatRequest;
import com.orasaka.core.model.OrasakaChatResponse;
import com.orasaka.core.model.OrasakaImageRequest;
import com.orasaka.core.model.OrasakaImageResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrasakaEngineTest {

  @Mock private ChatModel chatModel;
  @Mock private ImageModel imageModel;
  @Mock private EmbeddingModel embeddingModel;
  @Mock private TextToSpeechModel speechModel;
  @Mock private OrasakaToolRegistry toolRegistry;
  @Mock private OrasakaKnowledgeService knowledgeService;
  @Mock private McpOrchestrator mcpOrchestrator;
  @Mock private OrasakaMemoryResolver memoryResolver;
  @Mock private ApplicationEventPublisher eventPublisher;

  private OrasakaEngine engine;
  private CoreProperties properties;

  @BeforeEach
  void setUp() {
    properties =
        new CoreProperties(
            "ollama",
            Map.of(),
            new CoreProperties.RagConfig(false, null, 3),
            new CoreProperties.McpConfig(List.of()));

    OrasakaMemoryInterceptor memoryInterceptor = new OrasakaMemoryInterceptor(memoryResolver);
    OrasakaMcpInterceptor mcpInterceptor = new OrasakaMcpInterceptor(mcpOrchestrator);
    OrasakaRagInterceptor ragInterceptor = new OrasakaRagInterceptor(properties, knowledgeService);
    OrasakaToolInterceptor toolInterceptor = new OrasakaToolInterceptor(toolRegistry);

    engine =
        new OrasakaEngine(
            Map.of("ollama", chatModel),
            Map.of("ollama", imageModel),
            Map.of("ollama", embeddingModel),
            Map.of("ollama", speechModel),
            properties,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            eventPublisher);
  }

  @Test
  void shouldExecuteChatRequest() {
    // Given
    OrasakaChatRequest request = OrasakaChatRequest.simple("Hello");
    AssistantMessage assistantMessage = new AssistantMessage("Hi there!");
    Generation generation = new Generation(assistantMessage);
    ChatResponse chatResponse = new ChatResponse(List.of(generation));

    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    OrasakaChatResponse response = engine.chat(request);

    // Then
    assertThat(response.content()).isEqualTo("Hi there!");
    assertThat(response.metadata()).containsEntry("provider", "ollama");
    verify(chatModel).call(any(Prompt.class));
    verify(eventPublisher).publishEvent(any(Object.class));
  }

  @Test
  void shouldInjectRagContextWhenEnabled() {
    // Given
    properties =
        new CoreProperties(
            "ollama",
            Map.of(),
            new CoreProperties.RagConfig(true, "pgvector", 3),
            new CoreProperties.McpConfig(List.of()));

    OrasakaMemoryInterceptor memoryInterceptor = new OrasakaMemoryInterceptor(memoryResolver);
    OrasakaMcpInterceptor mcpInterceptor = new OrasakaMcpInterceptor(mcpOrchestrator);
    OrasakaRagInterceptor ragInterceptor = new OrasakaRagInterceptor(properties, knowledgeService);
    OrasakaToolInterceptor toolInterceptor = new OrasakaToolInterceptor(toolRegistry);

    engine =
        new OrasakaEngine(
            Map.of("ollama", chatModel),
            Map.of(),
            Map.of(),
            Map.of(),
            properties,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            eventPublisher);

    OrasakaChatRequest request = OrasakaChatRequest.simple("Hello");
    when(knowledgeService.retrieveContext(anyString(), anyInt())).thenReturn("Relevant context");

    AssistantMessage assistantMessage = new AssistantMessage("Response");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    engine.chat(request);

    // Then
    verify(knowledgeService).retrieveContext("Hello", 3);
    verify(chatModel)
        .call(
            argThat(
                (Prompt prompt) ->
                    prompt.getInstructions().stream()
                        .anyMatch(m -> m.getText().contains("RAG Context"))));
  }

  @Test
  void shouldInjectMcpContextWhenAvailable() {
    // Given
    OrasakaChatRequest request = OrasakaChatRequest.simple("Hello");
    when(mcpOrchestrator.resolveExternalContext()).thenReturn("External knowledge");

    AssistantMessage assistantMessage = new AssistantMessage("Response");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    engine.chat(request);

    // Then
    verify(mcpOrchestrator).resolveExternalContext();
    verify(chatModel)
        .call(
            argThat(
                (Prompt prompt) ->
                    prompt.getInstructions().stream()
                        .anyMatch(m -> m.getText().contains("MCP Context"))));
  }

  @Test
  void shouldThrowExceptionWhenProviderMissing() {
    // Given
    CoreProperties emptyProps = new CoreProperties(null, Map.of(), null, null);
    OrasakaEngine badEngine =
        new OrasakaEngine(
            Map.of(), Map.of(), Map.of(), Map.of(), emptyProps, List.of(), eventPublisher);

    // When / Then
    Assertions.assertThrows(
        OrasakaException.class, () -> badEngine.chat(OrasakaChatRequest.simple("test")));
  }

  @Test
  void shouldExecuteImageRequest() {
    // Given
    OrasakaImageRequest request =
        new OrasakaImageRequest("A beautiful sunset", 1024, 1024, null, null);
    ImageResponse imageResponse = mock(ImageResponse.class);
    ImageGeneration imgGen = mock(ImageGeneration.class);
    Image img = mock(Image.class);

    when(img.getUrl()).thenReturn("http://orasaka.ai/sunset.png");
    when(imgGen.getOutput()).thenReturn(img);
    when(imageResponse.getResults()).thenReturn(List.of(imgGen));
    when(imageResponse.getResult()).thenReturn(imgGen);
    when(imageModel.call(any(ImagePrompt.class))).thenReturn(imageResponse);

    // When
    OrasakaImageResponse response = engine.generateImage(request);

    // Then
    assertThat(response.url()).isEqualTo("http://orasaka.ai/sunset.png");
    verify(imageModel).call(any(ImagePrompt.class));
  }

  @Test
  void shouldBypassOrchestrationPipelineWhenDisabled() {
    // Given
    OrasakaChatRequest request = OrasakaChatRequest.simple("Fuzzy prompt");
    AssistantMessage assistantMessage = new AssistantMessage("Engine response");
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(assistantMessage))));

    // When
    OrasakaChatResponse response = engine.chat(request);

    // Then
    assertThat(response.content()).isEqualTo("Engine response");
    verify(chatModel)
        .call(
            argThat(
                (Prompt prompt) ->
                    prompt.getInstructions().stream()
                        .anyMatch(m -> m.getText().contains("Fuzzy prompt"))));
  }

  @Test
  void shouldExecutePipelineWhenEnabled() {
    // Given
    CoreProperties.OrchestrationConfig orchConfig =
        new CoreProperties.OrchestrationConfig(
            true,
            new CoreProperties.UserContextConfig(true),
            new CoreProperties.SystemContextConfig(true),
            new CoreProperties.RefinerConfig(true, "ollama", "llama-refine", 0.2),
            new CoreProperties.RouterConfig(true, "ollama", "llama-route", 0.0));
    CoreProperties enabledProps =
        new CoreProperties(
            "ollama",
            Map.of(),
            new CoreProperties.RagConfig(false, null, 3),
            new CoreProperties.McpConfig(List.of()),
            orchConfig);

    AssistantMessage refineResponse = new AssistantMessage("Refined prompt instruction");
    AssistantMessage routeResponse = new AssistantMessage("openai");

    ChatModel openaiChatModel = mock(ChatModel.class);

    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(refineResponse))))
        .thenReturn(new ChatResponse(List.of(new Generation(routeResponse))));

    AssistantMessage finalResponse = new AssistantMessage("Final response content");
    when(openaiChatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(finalResponse))));

    var userResolver = new com.orasaka.core.orchestration.UserContextResolver();
    var sysInjector =
        new com.orasaka.core.orchestration.SystemContextInjector(
            List.of(() -> Map.of("signal", "high-alert")));
    var refiner =
        new com.orasaka.core.orchestration.RefinerInterceptor(
            Map.of("ollama", chatModel), enabledProps);
    var router =
        new com.orasaka.core.orchestration.RouterInterceptor(
            Map.of("ollama", chatModel, "openai", openaiChatModel), enabledProps);

    var pipeline =
        new com.orasaka.core.orchestration.OrasakaOrchestrationPipeline(
            List.of(userResolver, sysInjector, refiner, router), enabledProps);

    OrasakaMemoryInterceptor memoryInterceptor = new OrasakaMemoryInterceptor(memoryResolver);
    OrasakaMcpInterceptor mcpInterceptor = new OrasakaMcpInterceptor(mcpOrchestrator);
    OrasakaRagInterceptor ragInterceptor =
        new OrasakaRagInterceptor(enabledProps, knowledgeService);
    OrasakaToolInterceptor toolInterceptor = new OrasakaToolInterceptor(toolRegistry);

    OrasakaEngine customEngine =
        new OrasakaEngine(
            Map.of("ollama", chatModel, "openai", openaiChatModel),
            Map.of(),
            Map.of(),
            Map.of(),
            enabledProps,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            pipeline,
            eventPublisher);

    OrasakaChatRequest request = OrasakaChatRequest.simple("Fuzzy prompt");

    // When
    OrasakaChatResponse response = customEngine.chat(request);

    // Then
    assertThat(response.content()).isEqualTo("Final response content");
    assertThat(response.metadata()).containsEntry("provider", "openai");

    verify(openaiChatModel)
        .call(
            argThat(
                (Prompt prompt) ->
                    prompt.getInstructions().stream()
                        .anyMatch(m -> m.getText().contains("Refined prompt instruction"))));
  }
}
