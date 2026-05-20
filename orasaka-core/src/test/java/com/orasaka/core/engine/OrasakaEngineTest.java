package com.orasaka.core.engine;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.mcp.McpOrchestrator;
import com.orasaka.core.model.OrasakaChatRequest;
import com.orasaka.core.model.OrasakaChatResponse;
import com.orasaka.core.rag.OrasakaKnowledgeService;
import com.orasaka.core.tool.OrasakaToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrasakaEngineTest {

    @Mock
    private ChatModel chatModel;
    @Mock
    private ImageModel imageModel;
    @Mock
    private EmbeddingModel embeddingModel;
    @Mock
    private TextToSpeechModel speechModel;
    @Mock
    private OrasakaToolRegistry toolRegistry;
    @Mock
    private OrasakaKnowledgeService knowledgeService;
    @Mock
    private McpOrchestrator mcpOrchestrator;
    @Mock
    private OrasakaMemoryResolver memoryResolver;

    private OrasakaEngine engine;
    private CoreProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CoreProperties(
                "ollama",
                Map.of(),
                new CoreProperties.RagConfig(false, null, 3),
                new CoreProperties.McpConfig(List.of())
        );

        engine = new OrasakaEngine(
                Map.of("ollama", chatModel),
                Map.of("ollama", imageModel),
                Map.of("ollama", embeddingModel),
                Map.of("ollama", speechModel),
                properties,
                toolRegistry,
                knowledgeService,
                mcpOrchestrator,
                memoryResolver
        );
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
    }

    @Test
    void shouldInjectRagContextWhenEnabled() {
        // Given
        properties = new CoreProperties(
                "ollama",
                Map.of(),
                new CoreProperties.RagConfig(true, "pgvector", 3),
                new CoreProperties.McpConfig(List.of())
        );
        engine = new OrasakaEngine(
                Map.of("ollama", chatModel), Map.of(), Map.of(), Map.of(),
                properties, toolRegistry, knowledgeService, mcpOrchestrator, memoryResolver
        );

        OrasakaChatRequest request = OrasakaChatRequest.simple("Hello");
        when(knowledgeService.retrieveContext(anyString(), anyInt())).thenReturn("Relevant context");
        
        AssistantMessage assistantMessage = new AssistantMessage("Response");
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // When
        engine.chat(request);

        // Then
        verify(knowledgeService).retrieveContext("Hello", 3);
        verify(chatModel).call(argThat((Prompt prompt) -> 
            prompt.getInstructions().stream().anyMatch(m -> m.getText().contains("RAG Context"))
        ));
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
        verify(chatModel).call(argThat((Prompt prompt) -> 
            prompt.getInstructions().stream().anyMatch(m -> m.getText().contains("MCP Context"))
        ));
    }

    @Test
    void shouldThrowExceptionWhenProviderMissing() {
        // Given
        CoreProperties emptyProps = new CoreProperties(null, Map.of(), null, null);
        OrasakaEngine badEngine = new OrasakaEngine(Map.of(), Map.of(), Map.of(), Map.of(), emptyProps, null, null, null, null);
        
        // When / Then
        org.junit.jupiter.api.Assertions.assertThrows(com.orasaka.core.exception.OrasakaException.class, () -> 
            badEngine.chat(OrasakaChatRequest.simple("test"))
        );
    }

    @Test
    void shouldExecuteImageRequest() {
        // Given
        com.orasaka.core.model.OrasakaImageRequest request = new com.orasaka.core.model.OrasakaImageRequest("A beautiful sunset", 1024, 1024, null, null);
        org.springframework.ai.image.ImageResponse imageResponse = mock(org.springframework.ai.image.ImageResponse.class);
        org.springframework.ai.image.ImageGeneration imgGen = mock(org.springframework.ai.image.ImageGeneration.class);
        org.springframework.ai.image.Image img = mock(org.springframework.ai.image.Image.class);
        
        when(img.getUrl()).thenReturn("http://orasaka.ai/sunset.png");
        when(imgGen.getOutput()).thenReturn(img);
        when(imageResponse.getResults()).thenReturn(List.of(imgGen));
        when(imageResponse.getResult()).thenReturn(imgGen);
        when(imageModel.call(any(org.springframework.ai.image.ImagePrompt.class))).thenReturn(imageResponse);

        // When
        com.orasaka.core.model.OrasakaImageResponse response = engine.generateImage(request);

        // Then
        assertThat(response.url()).isEqualTo("http://orasaka.ai/sunset.png");
        verify(imageModel).call(any(org.springframework.ai.image.ImagePrompt.class));
    }
}
