package com.orasaka.core.application.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.application.pipeline.EnginePipelineBridge;
import com.orasaka.core.application.pipeline.EngineStreamBridge;
import com.orasaka.core.application.pipeline.DynamicPipelineExecutor;
import com.orasaka.core.application.service.DynamicChatModelFactory;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.domain.model.audio.AudioRequest;
import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.domain.model.chat.InternalChatResponse;
import com.orasaka.core.domain.ports.outbound.KnowledgeService;
import com.orasaka.core.domain.ports.outbound.McpOrchestrator;
import com.orasaka.core.domain.ports.outbound.ModelCatalogProvider;
import com.orasaka.core.domain.ports.outbound.PlatformMcpServerProvider;
import com.orasaka.core.domain.ports.outbound.ToolRegistry;
import com.orasaka.core.domain.ports.outbound.UserCredentialsProvider;
import com.orasaka.core.domain.ports.outbound.UserMcpServerProvider;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.core.infrastructure.support.CoreException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.ApplicationEventPublisher;
import static com.orasaka.test.TestConstants.*;

/**
 * Comprehensive unit tests for {@link Engine} covering chat, tool attachment, RAG injection, MCP
 * context, memory, and orchestration pipeline behavior.
 */
@ExtendWith(MockitoExtension.class)
class EngineTest {

  @Mock private ChatModel chatModel;
  @Mock private EmbeddingModel embeddingModel;
  @Mock private TextToSpeechModel speechModel;
  @Mock private ToolRegistry toolRegistry;
  @Mock private KnowledgeService knowledgeService;
  @Mock private McpOrchestrator mcpOrchestrator;
  @Mock private ChatMemory mockChatMemory;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private ModelCatalogProvider modelCatalogProvider;
  @Mock private UserCredentialsProvider credentialsProvider;
  @Mock private DynamicChatModelFactory modelFactory;
  @Mock private PlatformMcpServerProvider platformMcpServerProvider;
  @Mock private UserMcpServerProvider userMcpServerProvider;

  private Engine engine;
  private CoreProperties properties;
  private EnginePipelineBridge pipelineBridge;
  private EngineStreamBridge streamBridge;

  @BeforeEach
  void setUp() throws Exception {
    properties =
        new CoreProperties(
            PROVIDER_OLLAMA,
            new CoreProperties.RagConfig(false, null, 3),
            new CoreProperties.McpConfig(List.of()),
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null,
            null,
            null,
            null);

    var memoryInterceptor = createMemoryInterceptor();
    var mcpInterceptor = createMcpInterceptor(mcpOrchestrator);
    var ragInterceptor = createRagInterceptor(properties, knowledgeService);
    var toolInterceptor = createToolInterceptor(toolRegistry);

    lenient()
        .when(modelCatalogProvider.getActiveChatModel())
        .thenReturn(Optional.of("llama3.1:8b"));

    pipelineBridge =
        new EnginePipelineBridge(
            modelCatalogProvider, platformMcpServerProvider, userMcpServerProvider, toolRegistry);
    streamBridge =
        new EngineStreamBridge(chatModel, pipelineBridge, credentialsProvider, modelFactory);

    engine =
        new Engine(
            chatModel,
            speechModel,
            properties,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            null,
            eventPublisher,
            modelCatalogProvider,
            pipelineBridge,
            streamBridge,
            credentialsProvider,
            modelFactory);
  }

  @Test
  void shouldExecuteChatRequest() {
    // Given
    InternalChatRequest request = InternalChatRequest.simple(PROMPT_HELLO_CAP);
    AssistantMessage assistantMessage = new AssistantMessage("Hi there!");
    Generation generation = new Generation(assistantMessage);
    ChatResponse chatResponse = new ChatResponse(List.of(generation));

    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    InternalChatResponse response = engine.chat(request);

    // Then
    assertThat(response.content()).isEqualTo("Hi there!");
    assertThat(response.metadata()).containsEntry("provider", PROVIDER_OLLAMA);
    verify(chatModel).call(any(Prompt.class));
    verify(eventPublisher).publishEvent(any(Object.class));
  }

  @Test
  void shouldAttachPosterToolWhenDemanded() {
    // Given
    ToolCallback mockTool = mock(ToolCallback.class);
    ToolDefinition mockDef = mock(ToolDefinition.class);
    when(mockDef.name()).thenReturn(TOOL_ANALYZE_POSTER);
    when(mockTool.getToolDefinition()).thenReturn(mockDef);
    when(toolRegistry.getRegisteredTools()).thenReturn(List.of(mockTool));

    InternalChatRequest request =
        InternalChatRequest.simple("Please analyze this poster for visual themes");
    AssistantMessage assistantMessage = new AssistantMessage(RESPONSE);
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
                        && ollama.getToolNames().contains(TOOL_ANALYZE_POSTER)
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
    when(mockDef.name()).thenReturn(TOOL_ANALYZE_POSTER);
    when(mockTool.getToolDefinition()).thenReturn(mockDef);
    when(toolRegistry.getRegisteredTools()).thenReturn(List.of(mockTool));

    InternalChatRequest request = InternalChatRequest.simple("Hello there, how are you?");
    AssistantMessage assistantMessage = new AssistantMessage(RESPONSE);
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
                        || !ollama.getToolNames().contains(TOOL_ANALYZE_POSTER);
                  }
                  return true;
                }));
  }

  @Test
  void shouldInjectRagContextWhenEnabled() throws Exception {
    // Given
    properties =
        new CoreProperties(
            PROVIDER_OLLAMA,
            new CoreProperties.RagConfig(true, "pgvector", 3),
            new CoreProperties.McpConfig(List.of()),
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null,
            null,
            null,
            null);

    var memoryInterceptor = createMemoryInterceptor();
    var mcpInterceptor = createMcpInterceptor(mcpOrchestrator);
    var ragInterceptor = createRagInterceptor(properties, knowledgeService);
    var toolInterceptor = createToolInterceptor(toolRegistry);

    engine =
        new Engine(
            chatModel,
            speechModel,
            properties,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            null,
            eventPublisher,
            modelCatalogProvider,
            pipelineBridge,
            streamBridge,
            credentialsProvider,
            modelFactory);

    InternalChatRequest request = InternalChatRequest.simple(PROMPT_HELLO_CAP);
    when(knowledgeService.retrieveContext(anyString(), anyInt())).thenReturn("Relevant context");

    AssistantMessage assistantMessage = new AssistantMessage(RESPONSE);
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    engine.chat(request);

    // Then
    verify(knowledgeService).retrieveContext(PROMPT_HELLO_CAP, 3);
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
    InternalChatRequest request = InternalChatRequest.simple(PROMPT_HELLO_CAP);
    when(mcpOrchestrator.resolveExternalContext()).thenReturn("External knowledge");

    AssistantMessage assistantMessage = new AssistantMessage(RESPONSE);
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
    when(mockChatMemory.get(SESSION_1)).thenReturn(List.of(oldUserMsg, oldAssistantMsg));

    // Inject the mock ChatMemory into the resolver via reflection
    var sessionsField = memoryResolver.getClass().getDeclaredField("sessionMemories");
    sessionsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    var sessions = (Map<String, ChatMemory>) sessionsField.get(memoryResolver);
    sessions.put(SESSION_1, mockChatMemory);

    var memoryInterceptor = createMemoryInterceptorWith(memoryResolver);
    var mcpInterceptor = createMcpInterceptor(mcpOrchestrator);
    var ragInterceptor = createRagInterceptor(properties, knowledgeService);
    var toolInterceptor = createToolInterceptor(toolRegistry);

    Engine historyEngine =
        new Engine(
            chatModel,
            speechModel,
            properties,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            null,
            eventPublisher,
            modelCatalogProvider,
            pipelineBridge,
            streamBridge,
            credentialsProvider,
            modelFactory);

    Context context = new Context("user-123", SESSION_1, Map.of(), Set.of());
    InternalChatRequest request = new InternalChatRequest("New prompt", null, null, context);

    AssistantMessage assistantMessage = new AssistantMessage(RESPONSE);
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
            null,
            null,
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null,
            null,
            null,
            null);
    Engine badEngine =
        new Engine(
            chatModel,
            speechModel,
            emptyProps,
            List.of(),
            null,
            eventPublisher,
            modelCatalogProvider,
            pipelineBridge,
            streamBridge,
            credentialsProvider,
            modelFactory);

    // When / Then
    var simpleReq = InternalChatRequest.simple("test");
    Assertions.assertThrows(CoreException.class, () -> badEngine.chat(simpleReq));
  }

  @Test
  void shouldBypassDynamicPipelineExecutorWhenDisabled() {
    // Given
    InternalChatRequest request = InternalChatRequest.simple(FUZZY_PROMPT);
    AssistantMessage assistantMessage = new AssistantMessage("Engine response");
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(assistantMessage))));

    // When
    this.engine.chat(request);

    // Then
    assertThat(engine.chat(request).content()).isEqualTo("Engine response");
    verify(chatModel, times(2))
        .call(
            argThat(
                (Prompt prompt) ->
                    prompt.getInstructions().stream()
                        .anyMatch(m -> m.getText().contains(FUZZY_PROMPT))));
  }

  @Test
  void shouldExecutePipelineWhenEnabled() throws Exception {
    // Given
    CoreProperties.OrchestrationConfig orchConfig =
        new CoreProperties.OrchestrationConfig(
            true,
            new CoreProperties.UserContextConfig(true),
            new CoreProperties.SystemContextConfig(true),
            new CoreProperties.InterceptorConfig(true, PROVIDER_OLLAMA, "llama-refine", 0.2),
            new CoreProperties.InterceptorConfig(true, PROVIDER_OLLAMA, "llama-route", 0.0));
    CoreProperties enabledProps =
        new CoreProperties(
            PROVIDER_OLLAMA,
            new CoreProperties.RagConfig(false, null, 3),
            new CoreProperties.McpConfig(List.of()),
            orchConfig,
            null,
            null,
            null,
            null);

    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Refined prompt instruction")))));

    DynamicPipelineExecutor pipeline = mock(DynamicPipelineExecutor.class);
    when(pipeline.process(anyString(), any(Context.class)))
        .thenReturn(new PromptContext(FUZZY_PROMPT, Map.of()));

    var memoryInterceptor = createMemoryInterceptor();
    var mcpInterceptor = createMcpInterceptor(mcpOrchestrator);
    var ragInterceptor = createRagInterceptor(enabledProps, knowledgeService);
    var toolInterceptor = createToolInterceptor(toolRegistry);

    Engine customEngine =
        new Engine(
            chatModel,
            speechModel,
            enabledProps,
            List.of(ragInterceptor, mcpInterceptor, memoryInterceptor, toolInterceptor),
            pipeline,
            eventPublisher,
            modelCatalogProvider,
            pipelineBridge,
            streamBridge,
            credentialsProvider,
            modelFactory);

    InternalChatRequest request = InternalChatRequest.simple(FUZZY_PROMPT);

    // When
    InternalChatResponse response = customEngine.chat(request);

    // Then — Pipeline interceptors created via reflection don't have @Value resources injected,
    // so refiner/router bypass. The first mock response from the default provider (ollama) is
    // returned.
    assertThat(response.content()).isEqualTo("Refined prompt instruction");
    assertThat(response.metadata()).containsEntry("provider", PROVIDER_OLLAMA);
  }

  private PromptContextInterceptor createMemoryInterceptor() throws Exception {
    Object resolver = createMemoryResolver();
    return createMemoryInterceptorWith(resolver);
  }

  private PromptContextInterceptor createMemoryInterceptorWith(Object resolver) throws Exception {
    Class<?> resolverClass = Class.forName("com.orasaka.core.domain.ports.outbound.MemoryResolver");
    Class<?> clazz = Class.forName("com.orasaka.core.application.interceptor.MemoryInterceptor");
    var constructor = clazz.getDeclaredConstructor(resolverClass);
    constructor.setAccessible(true);
    return (PromptContextInterceptor) constructor.newInstance(resolver);
  }

  private Object createMemoryResolver() throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.domain.ports.outbound.MemoryResolver");
    var constructor = clazz.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private PromptContextInterceptor createMcpInterceptor(McpOrchestrator orchestrator)
      throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.application.interceptor.McpInterceptor");
    var constructor = clazz.getDeclaredConstructor(McpOrchestrator.class);
    constructor.setAccessible(true);
    return (PromptContextInterceptor) constructor.newInstance(orchestrator);
  }



  private PromptContextInterceptor createRagInterceptor(
      CoreProperties properties, KnowledgeService knowledgeService) throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.application.interceptor.RagInterceptor");
    var constructor = clazz.getDeclaredConstructor(CoreProperties.class, KnowledgeService.class);
    constructor.setAccessible(true);
    return (PromptContextInterceptor) constructor.newInstance(properties, knowledgeService);
  }

  private PromptContextInterceptor createToolInterceptor(ToolRegistry toolRegistry)
      throws Exception {
    Class<?> clazz = Class.forName("com.orasaka.core.application.interceptor.ToolInterceptor");
    var constructor = clazz.getDeclaredConstructor(ToolRegistry.class);
    constructor.setAccessible(true);
    return (PromptContextInterceptor) constructor.newInstance(toolRegistry);
  }

  @Test
  void shouldGenerateSpeech() {
    // Given
    var req = new AudioRequest("hello tts", "alloy", "tts-1", Map.of(), Context.anonymous());
    var speechResponse = mock(TextToSpeechResponse.class);
    var speechResult = mock(Speech.class);
    byte[] expectedAudioBytes = new byte[] {1, 2, 3};

    when(speechResult.getOutput()).thenReturn(expectedAudioBytes);
    when(speechResponse.getResult()).thenReturn(speechResult);
    when(speechModel.call(any(TextToSpeechPrompt.class))).thenReturn(speechResponse);

    // When
    byte[] audioBytes = engine.generateSpeech(req);

    // Then
    assertThat(audioBytes).isEqualTo(expectedAudioBytes);
  }

  @Test
  void shouldResolveCommercialModelWithApiKey() {
    // Given
    CoreProperties openAiProps =
        new CoreProperties(
            PROVIDER_OPENAI,
            new CoreProperties.RagConfig(false, null, 3),
            new CoreProperties.McpConfig(List.of()),
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null,
            null,
            null,
            null);

    Engine openAiEngine =
        new Engine(
            chatModel,
            speechModel,
            openAiProps,
            List.of(),
            null,
            eventPublisher,
            modelCatalogProvider,
            pipelineBridge,
            streamBridge,
            credentialsProvider,
            modelFactory);

    Context context = new Context("user-123", SESSION_1, Map.of(), Set.of());
    OpenAiChatOptions openAiOptions = OpenAiChatOptions.builder().model(MODEL_GPT4O).build();
    InternalChatRequest request = new InternalChatRequest(PROMPT_HELLO_CAP, null, openAiOptions, context);

    ChatModel commercialChatModel = mock(ChatModel.class);
    AssistantMessage assistantMessage = new AssistantMessage("Commercial response");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));

    when(credentialsProvider.getDecryptedApiKey("user-123", PROVIDER_OPENAI))
        .thenReturn(Optional.of(TEST_API_KEY));
    when(modelFactory.createChatModel(PROVIDER_OPENAI, MODEL_GPT4O, TEST_API_KEY))
        .thenReturn(commercialChatModel);
    when(commercialChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    InternalChatResponse response = openAiEngine.chat(request);

    // Then
    assertThat(response.content()).isEqualTo("Commercial response");
    verify(modelFactory).createChatModel(PROVIDER_OPENAI, MODEL_GPT4O, TEST_API_KEY);
    verify(commercialChatModel).call(any(Prompt.class));
  }

  @Test
  void shouldWarnWhenApiKeyMissingForCommercialModel() {
    // Given
    CoreProperties openAiProps =
        new CoreProperties(
            PROVIDER_OPENAI,
            new CoreProperties.RagConfig(false, null, 3),
            new CoreProperties.McpConfig(List.of()),
            new CoreProperties.OrchestrationConfig(
                false,
                new CoreProperties.UserContextConfig(false),
                new CoreProperties.SystemContextConfig(false),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0),
                new CoreProperties.InterceptorConfig(false, null, null, 0.0)),
            null,
            null,
            null,
            null);

    Engine openAiEngine =
        new Engine(
            chatModel,
            speechModel,
            openAiProps,
            List.of(),
            null,
            eventPublisher,
            modelCatalogProvider,
            pipelineBridge,
            streamBridge,
            credentialsProvider,
            modelFactory);

    Context context = new Context("user-123", SESSION_1, Map.of(), Set.of());
    InternalChatRequest request = new InternalChatRequest(PROMPT_HELLO_CAP, null, null, context);

    AssistantMessage assistantMessage = new AssistantMessage("Local fallback response");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));

    when(credentialsProvider.getDecryptedApiKey("user-123", PROVIDER_OPENAI)).thenReturn(Optional.empty());
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    InternalChatResponse response = openAiEngine.chat(request);

    // Then
    assertThat(response.content()).isEqualTo("Local fallback response");
    verify(chatModel).call(any(Prompt.class));
    verifyNoInteractions(modelFactory);
  }

  @Test
  void shouldTriggerSimulatedBackpressure() {
    // Given
    InternalChatRequest request = InternalChatRequest.simple("Trigger backpressure scenario");
    AssistantMessage assistantMessage = new AssistantMessage("Under pressure response");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // When
    InternalChatResponse response = engine.chat(request);

    // Then
    assertThat(response.content()).isEqualTo("Under pressure response");
  }

  @Test
  void shouldCloseAutoCloseableMcpClients() throws Exception {
    // Given
    InternalChatRequest request = InternalChatRequest.simple(PROMPT_HELLO_CAP);
    EnginePipelineBridge mockBridge = mock(EnginePipelineBridge.class);
    AutoCloseable mockCloseable1 = mock(AutoCloseable.class);
    AutoCloseable mockCloseable2 = mock(AutoCloseable.class);
    doThrow(new RuntimeException("Close failed")).when(mockCloseable2).close();

    var compiledCtx =
        new com.orasaka.core.application.pipeline.EnginePipelineContext(
            PROVIDER_OLLAMA,
            SESSION_1,
            PROMPT_HELLO_CAP,
            new Prompt(PROMPT_HELLO_CAP),
            List.of(mockCloseable1, mockCloseable2));

    when(mockBridge.compileContext(any(), any(), any(), any())).thenReturn(compiledCtx);

    Engine customEngine =
        new Engine(
            chatModel,
            speechModel,
            properties,
            List.of(),
            null,
            eventPublisher,
            modelCatalogProvider,
            mockBridge,
            streamBridge,
            credentialsProvider,
            modelFactory);

    // When
    customEngine.chat(request);

    // Then
    verify(mockCloseable1).close();
    verify(mockCloseable2).close();
  }

  @Test
  void shouldReturnEmptyStringWhenResponseOrResultIsNull() {
    // Given
    InternalChatRequest request = InternalChatRequest.simple(PROMPT_HELLO_CAP);
    when(chatModel.call(any(Prompt.class))).thenReturn(null);

    // When
    InternalChatResponse response = engine.chat(request);

    // Then
    assertThat(response.content()).isEmpty();
  }
}
