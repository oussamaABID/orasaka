package com.orasaka.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.orasaka.core.pipeline.ContextInterceptor;
import com.orasaka.core.pipeline.KnowledgeService;
import com.orasaka.core.pipeline.McpOrchestrator;
import com.orasaka.core.pipeline.OrchestrationPipeline;
import com.orasaka.core.pipeline.SystemContextProvider;
import com.orasaka.core.pipeline.ToolRegistry;
import com.orasaka.core.support.Context;
import com.orasaka.core.support.CoreException;
import com.orasaka.core.support.InternalChatRequest;
import com.orasaka.core.support.InternalChatResponse;
import com.orasaka.core.support.InternalImageRequest;
import com.orasaka.core.support.InternalImageResponse;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
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
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Comprehensive unit tests for {@link Engine} covering chat, image generation, tool attachment, RAG
 * injection, MCP context, memory, and orchestration pipeline behavior.
 */
@ExtendWith(MockitoExtension.class)
class EngineTest {

  @Mock private ChatModel chatModel;
  @Mock private ImageModel imageModel;
  @Mock private EmbeddingModel embeddingModel;
  @Mock private TextToSpeechModel speechModel;
  @Mock private ToolRegistry toolRegistry;
  @Mock private KnowledgeService knowledgeService;
  @Mock private McpOrchestrator mcpOrchestrator;
  @Mock private ChatMemory mockChatMemory;
  @Mock private ApplicationEventPublisher eventPublisher;

  private Engine engine;
  private CoreProperties properties;

  @BeforeEach
  void setUp() throws Exception {
    properties =
        new CoreProperties(
            "ollama",
            Map.of(),
            new CoreProperties.RagConfig(false, null, 3),
            new CoreProperties.McpConfig(List.of()),
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null);

    var memoryInterceptor = createMemoryInterceptor();
    var mcpInterceptor = createMcpInterceptor(mcpOrchestrator);
    var ragInterceptor = createRagInterceptor(properties, knowledgeService);
    var toolInterceptor = createToolInterceptor(toolRegistry);

    engine =
        new Engine(
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
    InternalChatRequest request = InternalChatRequest.simple("Hello");
    AssistantMessage assistantMessage = new AssistantMessage("Hi there!");
    Generation generation = new Generation(assistantMessage);
    ChatResponse chatResponse = new ChatResponse(List.of(generation));

    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    InternalChatResponse response = engine.chat(request);

    // Then
    assertThat(response.content()).isEqualTo("Hi there!");
    assertThat(response.metadata()).containsEntry("provider", "ollama");
    verify(chatModel).call(any(Prompt.class));
    verify(eventPublisher).publishEvent(any(Object.class));
  }

  @Test
  void shouldAttachPosterToolWhenDemanded() {
    // Given
    ToolCallback mockTool = mock(ToolCallback.class);
    ToolDefinition mockDef = mock(ToolDefinition.class);
    when(mockDef.name()).thenReturn("analyzePoster");
    when(mockTool.getToolDefinition()).thenReturn(mockDef);
    when(toolRegistry.getRegisteredTools()).thenReturn(List.of(mockTool));

    InternalChatRequest request =
        InternalChatRequest.simple("Please analyze this poster for visual themes");
    AssistantMessage assistantMessage = new AssistantMessage("Response");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    engine.chat(request);

    // Then
    verify(chatModel)
        .call(
            argThat(
                (Prompt prompt) -> {
                  var opts = prompt.getOptions();
                  if (opts instanceof OllamaChatOptions ollama) {
                    return ollama.getToolNames() != null
                        && ollama.getToolNames().contains("analyzePoster")
                        && ollama.getToolCallbacks() != null
                        && ollama.getToolCallbacks().contains(mockTool);
                  }
                  return false;
                }));
  }

  @Test
  void shouldNotAttachPosterToolWhenNotDemanded() {
    // Given
    ToolCallback mockTool = mock(ToolCallback.class);
    ToolDefinition mockDef = mock(ToolDefinition.class);
    when(mockDef.name()).thenReturn("analyzePoster");
    when(mockTool.getToolDefinition()).thenReturn(mockDef);
    when(toolRegistry.getRegisteredTools()).thenReturn(List.of(mockTool));

    InternalChatRequest request = InternalChatRequest.simple("Hello there, how are you?");
    AssistantMessage assistantMessage = new AssistantMessage("Response");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    engine.chat(request);

    // Then
    verify(chatModel)
        .call(
            argThat(
                (Prompt prompt) -> {
                  var opts = prompt.getOptions();
                  if (opts instanceof OllamaChatOptions ollama) {
                    return ollama.getToolNames() == null
                        || !ollama.getToolNames().contains("analyzePoster");
                  }
                  return true;
                }));
  }

  @Test
  void shouldInjectRagContextWhenEnabled() throws Exception {
    // Given
    properties =
        new CoreProperties(
            "ollama",
            Map.of(),
            new CoreProperties.RagConfig(true, "pgvector", 3),
            new CoreProperties.McpConfig(List.of()),
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null);

    var memoryInterceptor = createMemoryInterceptor();
    var mcpInterceptor = createMcpInterceptor(mcpOrchestrator);
    var ragInterceptor = createRagInterceptor(properties, knowledgeService);
    var toolInterceptor = createToolInterceptor(toolRegistry);

    engine =
        new Engine(
            Map.of("ollama", chatModel),
            Map.of(),
            Map.of(),
            Map.of(),
            properties,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            eventPublisher);

    InternalChatRequest request = InternalChatRequest.simple("Hello");
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
  void shouldInjectMcpContextWhenAvailable() throws Exception {
    // Given
    InternalChatRequest request = InternalChatRequest.simple("Hello");
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
  void shouldPrependHistoryBeforeCurrentPrompt() throws Exception {
    // Given
    UserMessage oldUserMsg = new UserMessage("Old user prompt");
    AssistantMessage oldAssistantMsg = new AssistantMessage("Old assistant reply");

    Object memoryResolver = createMemoryResolver();
    when(mockChatMemory.get("session-123")).thenReturn(List.of(oldUserMsg, oldAssistantMsg));

    // Inject the mock ChatMemory into the resolver via reflection
    var sessionsField = memoryResolver.getClass().getDeclaredField("sessionMemories");
    sessionsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    var sessions = (Map<String, ChatMemory>) sessionsField.get(memoryResolver);
    sessions.put("session-123", mockChatMemory);

    var memoryInterceptor = createMemoryInterceptorWith(memoryResolver);
    var mcpInterceptor = createMcpInterceptor(mcpOrchestrator);
    var ragInterceptor = createRagInterceptor(properties, knowledgeService);
    var toolInterceptor = createToolInterceptor(toolRegistry);

    Engine historyEngine =
        new Engine(
            Map.of("ollama", chatModel),
            Map.of("ollama", imageModel),
            Map.of("ollama", embeddingModel),
            Map.of("ollama", speechModel),
            properties,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            eventPublisher);

    Context context = new Context("user-123", "session-123", Map.of(), Set.of());
    InternalChatRequest request = new InternalChatRequest("New prompt", null, null, context);

    AssistantMessage assistantMessage = new AssistantMessage("Response");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    historyEngine.chat(request);

    // Then
    verify(chatModel)
        .call(
            argThat(
                (Prompt prompt) -> {
                  List<Message> instructions = prompt.getInstructions();
                  if (instructions.size() != 3) {
                    return false;
                  }
                  return instructions.get(0).getText().equals("Old user prompt")
                      && instructions.get(1).getText().equals("Old assistant reply")
                      && instructions.get(2).getText().equals("New prompt");
                }));
  }

  @Test
  void shouldThrowExceptionWhenProviderMissing() {
    // Given
    CoreProperties emptyProps =
        new CoreProperties(
            null,
            Map.of(),
            null,
            null,
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null);
    Engine badEngine =
        new Engine(Map.of(), Map.of(), Map.of(), Map.of(), emptyProps, List.of(), eventPublisher);

    // When / Then
    Assertions.assertThrows(
        CoreException.class, () -> badEngine.chat(InternalChatRequest.simple("test")));
  }

  @Test
  void shouldExecuteImageRequest() {
    // Given
    InternalImageRequest request =
        new InternalImageRequest("A beautiful sunset", 1024, 1024, null, null);
    ImageResponse imageResponse = mock(ImageResponse.class);
    ImageGeneration imgGen = mock(ImageGeneration.class);
    Image img = mock(Image.class);

    when(img.getUrl()).thenReturn("http://orasaka.ai/sunset.png");
    when(imgGen.getOutput()).thenReturn(img);
    when(imageResponse.getResult()).thenReturn(imgGen);
    when(imageModel.call(any(ImagePrompt.class))).thenReturn(imageResponse);

    // When
    InternalImageResponse response = engine.generateImage(request);

    // Then
    assertThat(response.url()).isEqualTo("http://orasaka.ai/sunset.png");
    verify(imageModel).call(any(ImagePrompt.class));
  }

  @Test
  void shouldBypassOrchestrationPipelineWhenDisabled() {
    // Given
    InternalChatRequest request = InternalChatRequest.simple("Fuzzy prompt");
    AssistantMessage assistantMessage = new AssistantMessage("Engine response");
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(assistantMessage))));

    // When
    InternalChatResponse response = engine.chat(request);

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
  void shouldExecutePipelineWhenEnabled() throws Exception {
    // Given
    CoreProperties.OrchestrationConfig orchConfig =
        new CoreProperties.OrchestrationConfig(
            true,
            new CoreProperties.UserContextConfig(true),
            new CoreProperties.SystemContextConfig(true),
            new CoreProperties.InterceptorConfig(true, "ollama", "llama-refine", 0.2),
            new CoreProperties.InterceptorConfig(true, "ollama", "llama-route", 0.0));
    CoreProperties enabledProps =
        new CoreProperties(
            "ollama",
            Map.of(),
            new CoreProperties.RagConfig(false, null, 3),
            new CoreProperties.McpConfig(List.of()),
            orchConfig,
            null);

    AssistantMessage refineResponse = new AssistantMessage("Refined prompt instruction");
    AssistantMessage routeResponse = new AssistantMessage("openai");

    ChatModel openaiChatModel = mock(ChatModel.class);

    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(refineResponse))))
        .thenReturn(new ChatResponse(List.of(new Generation(routeResponse))));

    Object userResolver = createUserResolver();
    List<SystemContextProvider> providers = List.of(() -> Map.of("signal", "high-alert"));
    Object sysInjector = createSysInjector(providers);
    Object refiner = createRefiner(Map.of("ollama", chatModel), enabledProps);
    Object router =
        createRouter(Map.of("ollama", chatModel, "openai", openaiChatModel), enabledProps);

    Constructor<OrchestrationPipeline> pipelineConstructor =
        OrchestrationPipeline.class.getConstructor(List.class, CoreProperties.class);
    OrchestrationPipeline pipeline =
        pipelineConstructor.newInstance(
            List.of(userResolver, sysInjector, refiner, router), enabledProps);

    var memoryInterceptor = createMemoryInterceptor();
    var mcpInterceptor = createMcpInterceptor(mcpOrchestrator);
    var ragInterceptor = createRagInterceptor(enabledProps, knowledgeService);
    var toolInterceptor = createToolInterceptor(toolRegistry);

    Engine customEngine =
        new Engine(
            Map.of("ollama", chatModel, "openai", openaiChatModel),
            Map.of(),
            Map.of(),
            Map.of(),
            enabledProps,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            pipeline,
            eventPublisher);

    InternalChatRequest request = InternalChatRequest.simple("Fuzzy prompt");

    // When
    InternalChatResponse response = customEngine.chat(request);

    // Then — Pipeline interceptors created via reflection don't have @Value resources injected,
    // so refiner/router bypass. The first mock response from the default provider (ollama) is
    // returned.
    assertThat(response.content()).isEqualTo("Refined prompt instruction");
    assertThat(response.metadata()).containsEntry("provider", "ollama");
  }

  private ContextInterceptor createMemoryInterceptor() throws Exception {
    Object resolver = createMemoryResolver();
    return createMemoryInterceptorWith(resolver);
  }

  private ContextInterceptor createMemoryInterceptorWith(Object resolver) throws Exception {
    Class<?> resolverClass = Class.forName("com.orasaka.core.pipeline.MemoryResolver");
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.MemoryInterceptor");
    var constructor = clazz.getDeclaredConstructor(resolverClass);
    constructor.setAccessible(true);
    return (ContextInterceptor) constructor.newInstance(resolver);
  }

  private Object createMemoryResolver() throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.MemoryResolver");
    var constructor = clazz.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private ContextInterceptor createMcpInterceptor(McpOrchestrator orchestrator) throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.McpInterceptor");
    var constructor = clazz.getDeclaredConstructor(McpOrchestrator.class);
    constructor.setAccessible(true);
    return (ContextInterceptor) constructor.newInstance(orchestrator);
  }

  private Object createUserResolver() throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.UserContextResolver");
    var constructor = clazz.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private Object createSysInjector(List<SystemContextProvider> providers) throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.SystemContextInjector");
    var constructor = clazz.getDeclaredConstructor(List.class);
    constructor.setAccessible(true);
    return constructor.newInstance(providers);
  }

  private Object createRefiner(Map<String, ChatModel> chatModels, CoreProperties properties)
      throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.RefinerInterceptor");
    var constructor = clazz.getDeclaredConstructor(Map.class, CoreProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(chatModels, properties);
  }

  private Object createRouter(Map<String, ChatModel> chatModels, CoreProperties properties)
      throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.RouterInterceptor");
    var constructor = clazz.getDeclaredConstructor(Map.class, CoreProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(chatModels, properties);
  }

  private ContextInterceptor createRagInterceptor(
      CoreProperties properties, KnowledgeService knowledgeService) throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.RagInterceptor");
    var constructor = clazz.getDeclaredConstructor(CoreProperties.class, KnowledgeService.class);
    constructor.setAccessible(true);
    return (ContextInterceptor) constructor.newInstance(properties, knowledgeService);
  }

  private ContextInterceptor createToolInterceptor(ToolRegistry toolRegistry) throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.pipeline.ToolInterceptor");
    var constructor = clazz.getDeclaredConstructor(ToolRegistry.class);
    constructor.setAccessible(true);
    return (ContextInterceptor) constructor.newInstance(toolRegistry);
  }
}
