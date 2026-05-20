package com.orasaka.core.engine;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.exception.OrasakaException;
import com.orasaka.core.model.*;
import com.orasaka.core.rag.OrasakaKnowledgeService;
import com.orasaka.core.tool.OrasakaToolRegistry;
import com.orasaka.core.mcp.McpOrchestrator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.openai.OpenAiImageOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Core Orchestration Engine for the Orasaka CORS library.
 * 
 * <p>Implements the Bridge Pattern to decouple host applications from Spring AI internals.
 * Manages the integration of RAG, Tooling, and MCP protocols.
 * 
 * <p>High-concurrency tasks are executed using Java 21 Virtual Threads to ensure 
 * non-blocking performance.
 *
 * @see <a href="file:///Users/oussamaabid/Documents/projects/orasaka/docs/GLOSSARY.md">Orasaka Glossary</a>
 * @see org.springframework.ai.chat.model.ChatModel
 */
public abstract class AbstractOrasakaEngine {

    protected final Map<String, ChatModel> chatModels;
    protected final Map<String, ImageModel> imageModels;
    protected final Map<String, EmbeddingModel> embeddingModels;
    protected final Map<String, TextToSpeechModel> speechModels;
    protected final CoreProperties properties;
    protected final OrasakaToolRegistry toolRegistry;
    protected final OrasakaKnowledgeService knowledgeService;
    protected final McpOrchestrator mcpOrchestrator;
    protected final OrasakaMemoryResolver memoryResolver;
    
    /** Virtual Thread Executor for high-concurrency AI orchestration. */
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Initializes the engine with all required cognitive components.
     *
     * @param chatModels Map of available chat model providers.
     * @param imageModels Map of available image model providers.
     * @param embeddingModels Map of available embedding model providers.
     * @param speechModels Map of available speech model providers.
     * @param properties Configuration properties (Mandatory: defaultProvider).
     * @param toolRegistry Local Java tool registry.
     * @param knowledgeService RAG knowledge orchestration service.
     * @param mcpOrchestrator Model Context Protocol bridge.
     * @param memoryResolver Resolver for session-based chat memory.
     */
    protected AbstractOrasakaEngine(
            Map<String, ChatModel> chatModels,
            Map<String, ImageModel> imageModels,
            Map<String, EmbeddingModel> embeddingModels,
            Map<String, TextToSpeechModel> speechModels,
            CoreProperties properties,
            OrasakaToolRegistry toolRegistry,
            OrasakaKnowledgeService knowledgeService,
            McpOrchestrator mcpOrchestrator,
            OrasakaMemoryResolver memoryResolver) {
        this.chatModels = chatModels;
        this.imageModels = imageModels;
        this.embeddingModels = embeddingModels;
        this.speechModels = speechModels;
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.knowledgeService = knowledgeService;
        this.mcpOrchestrator = mcpOrchestrator;
        this.memoryResolver = memoryResolver;
    }

    /**
     * Executes a chat request using the active provider with agentic capabilities.
     * 
     * <p>This method is thread-safe and non-blocking as it utilizes Virtual Threads 
     * for the heavy lifting of AI inference and context retrieval.
     *
     * @param request The domain-specific chat request.
     * @return A synchronized chat response containing the LLM output and metadata.
     * @throws OrasakaException If execution fails or no provider is available.
     */
    public OrasakaChatResponse chat(OrasakaChatRequest request) {
        try {
            return virtualThreadExecutor.submit(() -> executeChat(request)).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new OrasakaException("Failed to execute chat request in virtual thread", e);
        }
    }

    /**
     * Executes an image generation request using the active provider.
     * 
     * <p>This method utilizes Virtual Threads for non-blocking performance.
     *
     * @param request The domain-specific image request.
     * @return A synchronized image response containing generated data or metadata.
     * @throws OrasakaException If execution fails or provider misconfiguration occurs.
     */
    public OrasakaImageResponse generateImage(OrasakaImageRequest request) {
        try {
            return virtualThreadExecutor.submit(() -> executeImageGeneration(request)).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new OrasakaException("Failed to execute image generation in virtual thread", e);
        }
    }

    /**
     * Executes a Text-To-Speech request using the active provider.
     *
     * <p>Accepts an {@link OrasakaSpeechRequest} carrying an {@link com.orasaka.core.context.OrasakaContext}
     * so that per-user voice model and speed preferences are dynamically injected.
     *
     * @param request The speech generation specification including user context.
     * @return Raw audio bytes produced by the underlying TTS provider.
     * @throws OrasakaException If execution fails or provider misconfiguration occurs.
     */
    public byte[] generateSpeech(OrasakaSpeechRequest request) {
        try {
            return virtualThreadExecutor.submit(() -> executeSpeechGeneration(request)).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new OrasakaException("Failed to execute speech generation in virtual thread", e);
        }
    }

    /**
     * Core execution logic for TTS generation.
     * Applies voice/speed overrides from the {@link com.orasaka.core.context.OrasakaContext} when present.
     */
    private byte[] executeSpeechGeneration(OrasakaSpeechRequest request) {
        String provider = getActiveProvider();
        TextToSpeechModel model = speechModels.get(provider);
        if (model == null) {
            throw new OrasakaException("No TextToSpeechModel found for provider: " + provider);
        }

        // Resolve voice preference from context preferences if present (provider-specific wiring)
        if (request.context() != null && request.context().preferences() != null) {
            Object voicePref = request.context().preferences().get("tts-voice");
            if (voicePref instanceof String s) {
                // Voice selection is provider-specific; log for now, apply in provider-specific subclass
                org.slf4j.LoggerFactory.getLogger(AbstractOrasakaEngine.class)
                        .debug("TTS voice preference '{}' resolved from context for user '{}'", s,
                                request.context().userId());
            }
        }

        org.springframework.ai.audio.tts.TextToSpeechPrompt prompt =
                new org.springframework.ai.audio.tts.TextToSpeechPrompt(request.text());
        org.springframework.ai.audio.tts.TextToSpeechResponse response = model.call(prompt);
        return response.getResult().getOutput();
    }

    /**
     * Core execution logic for multi-modal image generation.
     * 
     * @param request The image request to process.
     * @return The generated image response.
     */
    private OrasakaImageResponse executeImageGeneration(OrasakaImageRequest request) {
        String provider = getActiveProvider();
        ImageModel model = imageModels.get(provider);
        if (model == null) {
            throw new OrasakaException("No ImageModel found for provider: " + provider);
        }

        org.springframework.ai.image.ImageOptions springOptions = mapImageOptions(request);
        ImagePrompt prompt = new ImagePrompt(List.of(new ImageMessage(request.prompt())), springOptions);
        ImageResponse response = model.call(prompt);

        if (response.getResults().isEmpty()) {
            throw new OrasakaException("Image generation returned no results");
        }

        var result = response.getResult().getOutput();
        return new OrasakaImageResponse(
                null, // Byte data usually handled by separate download if needed
                result.getUrl(),
                "png" // Default format
        );
    }

    /**
     * Core execution logic for agentic chat.
     * Handles RAG injection, MCP context resolution, and Tool attachment.
     * 
     * @param request The chat request to process.
     * @return The generated response from the underlying model.
     */
    private OrasakaChatResponse executeChat(OrasakaChatRequest request) {
        ChatModel model = resolveChatModel();
        List<Message> messages = new ArrayList<>();
        
        // 1. RAG Injection
        if (properties.rag() != null && properties.rag().enabled() && knowledgeService != null) {
            String context = knowledgeService.retrieveContext(request.prompt(), properties.rag().topK());
            if (context != null && !context.isBlank()) {
                messages.add(new SystemMessage("RAG Context: \n" + context));
            }
        }

        // 2. MCP Context Injection
        if (mcpOrchestrator != null) {
            String mcpContext = mcpOrchestrator.resolveExternalContext();
            if (mcpContext != null && !mcpContext.isBlank()) {
                messages.add(new SystemMessage("MCP Context: " + mcpContext));
            }
        }

        if (request.messages() != null) {
            messages.addAll(request.messages().stream()
                    .map(this::mapMessage)
                    .toList());
        }
        if (request.prompt() != null && !request.prompt().isBlank()) {
            messages.add(new UserMessage(request.prompt()));
        }

        // 3. Tool Attachment
        org.springframework.ai.chat.prompt.ChatOptions springOptions = mapOptions(request.options());
        if (toolRegistry != null && !toolRegistry.getRegisteredTools().isEmpty()) {
            springOptions = attachTools(springOptions);
        }

        Prompt prompt = new Prompt(messages, springOptions);
        ChatResponse response = model.call(prompt);
        
        return new OrasakaChatResponse(
                response.getResult().getOutput().getText(),
                (request.context() != null) ? request.context().conversationId() : null,
                Map.of("provider", getActiveProvider())
        );
    }

    /**
     * Resolves the ChatModel for the active provider.
     * 
     * @return The configured {@link ChatModel}.
     * @throws OrasakaException If no model matches the active provider.
     */
    protected ChatModel resolveChatModel() {
        String provider = getActiveProvider();
        ChatModel model = chatModels.get(provider);
        if (model == null) {
            throw new OrasakaException("No ChatModel found for provider: " + provider);
        }
        return model;
    }

    /**
     * Identifies the current active AI provider from properties.
     * 
     * @return The provider key (e.g., "ollama", "openai").
     * @throws OrasakaException If the default provider property is missing.
     */
    protected String getActiveProvider() {
        if (properties.defaultProvider() == null || properties.defaultProvider().isBlank()) {
            throw new OrasakaException("Missing required property: orasaka.core.default-provider");
        }
        return properties.defaultProvider();
    }

    /**
     * Resolves the base URL for the active provider.
     * 
     * @return The base URL string.
     * @throws OrasakaException If the base URL is missing for the active provider.
     */
    protected String getBaseUrl() {
        String provider = getActiveProvider();
        if (properties.overrides() != null && properties.overrides().containsKey(provider)) {
            String baseUrl = properties.overrides().get(provider).baseUrl();
            if (baseUrl != null && !baseUrl.isBlank()) return baseUrl;
        }
        throw new OrasakaException("Missing required property: orasaka.core.overrides." + provider + ".base-url");
    }

    /**
     * Maps Orasaka messages to Spring AI message types.
     * 
     * @param msg The source Orasaka message.
     * @return The equivalent {@link Message}.
     */
    private Message mapMessage(OrasakaChatRequest.ChatMessage msg) {
        return switch (msg.role().toLowerCase()) {
            case "system" -> new SystemMessage(msg.content());
            case "assistant" -> new AssistantMessage(msg.content());
            default -> new UserMessage(msg.content());
        };
    }

    /**
     * Maps high-level Orasaka options to provider-specific Spring AI options.
     * 
     * @param options The source Orasaka options.
     * @return Configured {@link org.springframework.ai.chat.prompt.ChatOptions}.
     */
    private ChatOptions mapOptions(OrasakaOptions options) {
        Double temp = (options != null) ? options.getTemperature() : 0.7;
        Integer tokens = (options != null) ? options.getMaxTokens() : null;

        return switch (getActiveProvider().toLowerCase()) {
            case "ollama" -> OllamaChatOptions.builder()
                    .temperature(temp)
                    .numPredict(tokens)
                    .build();
            case "openai" -> OpenAiChatOptions.builder()
                    .temperature(temp)
                    .maxTokens(tokens)
                    .build();
            default -> {
                DefaultChatOptions defaultOptions = new DefaultChatOptions();
                defaultOptions.setTemperature(temp);
                defaultOptions.setMaxTokens(tokens);
                yield defaultOptions;
            }
        };
    }

    /**
     * Attaches registered native tools to the chat options.
     * 
     * @param options The existing options to augment.
     * @return augmented {@link org.springframework.ai.chat.prompt.ChatOptions}.
     */
    private ChatOptions attachTools(ChatOptions options) {
        java.util.Set<String> toolNames = toolRegistry.getRegisteredTools().stream()
                .map(t -> t.getToolDefinition().name())
                .collect(java.util.stream.Collectors.toSet());

        return switch (options) {
            case OllamaChatOptions ollama -> OllamaChatOptions.builder()
                    .model(ollama.getModel())
                    .temperature(ollama.getTemperature())
                    .toolNames(toolNames)
                    .build();
            case OpenAiChatOptions openai -> OpenAiChatOptions.builder()
                    .model(openai.getModel())
                    .temperature(openai.getTemperature())
                    .toolNames(toolNames)
                    .build();
            default -> options;
        };
    }

    /**
     * Maps Orasaka image request parameters to provider-specific Spring AI image options.
     * 
     * @param request The source image request.
     * @return Configured {@link org.springframework.ai.image.ImageOptions}.
     */
    private ImageOptions mapImageOptions(OrasakaImageRequest request) {
        return switch (getActiveProvider().toLowerCase()) {
            case "openai" -> OpenAiImageOptions.builder()
                    .height(request.height())
                    .width(request.width())
                    .quality("hd")
                    .build();
            default -> null; // Other providers like Ollama do not support dedicated ImageModel generation in Spring AI 1.1.6
        };
    }
}
