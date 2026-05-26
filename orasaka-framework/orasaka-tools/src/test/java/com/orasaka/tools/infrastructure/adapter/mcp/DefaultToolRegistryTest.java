package com.orasaka.tools.infrastructure.adapter.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.ports.outbound.KnowledgeService;
import com.orasaka.core.domain.ports.outbound.PlatformToolConfigProvider;
import com.orasaka.core.infrastructure.support.SecurityContextUtil;
import com.orasaka.tools.application.service.CachingToolCallback;
import com.orasaka.tools.application.service.ToolCacheService;
import com.orasaka.tools.infrastructure.adapter.persistence.entity.ToolRagSourceEntity;
import com.orasaka.tools.infrastructure.adapter.persistence.repository.ToolRagSourceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.dao.DataRetrievalFailureException;

@ExtendWith(MockitoExtension.class)
class DefaultToolRegistryTest {

  @Mock private PlatformToolConfigProvider platformToolConfigProvider;

  @Mock private ToolCacheService cacheService;

  @Mock private ToolRagSourceRepository ragSourceRepository;

  @Mock private KnowledgeService knowledgeService;

  private DefaultToolRegistry registry;

  @BeforeEach
  void setUp() {
    registry =
        new DefaultToolRegistry(
            platformToolConfigProvider, cacheService, ragSourceRepository, knowledgeService);
    registry.registerDefaultTools();
  }

  @Test
  void shouldRegisterTool() {
    String name = "testTool";
    String description = "A test tool";
    Function<String, String> function = input -> "Hello " + input;

    registry.registerTool(name, description, String.class, function);

    List<ToolCallback> tools = registry.getRegisteredTools();
    assertThat(tools).hasSize(4);
    assertThat(tools.stream().map(t -> t.getToolDefinition().name()))
        .contains(name, "analyzePoster", "analyzeAudioExtract", "searchWeb");
  }

  @Test
  void shouldReturnImmutableList() {
    registry.registerTool("tool1", "desc1", String.class, (String s) -> s);
    List<ToolCallback> tools = registry.getRegisteredTools();
    assertThrows(UnsupportedOperationException.class, () -> tools.add(null));
  }

  @ParameterizedTest
  @CsvSource({
    "analyzePoster, '{\"posterBase64\":\"valid-base64\",\"prompt\":\"cat\"}', 'Poster analysis success'",
    "analyzePoster, '{\"posterBase64\":\"corrupt-data\",\"prompt\":\"cat\"}', 'Failed to parse poster image'",
    "analyzeAudioExtract, '{\"clipPath\":\"valid-path.mp3\",\"checkType\":\"loudness\"}', 'Audio extract compliance check passed'",
    "analyzeAudioExtract, '{\"clipPath\":\"corrupt-path.mp3\",\"checkType\":\"loudness\"}', 'Audio clip corrupt or missing'"
  })
  void testToolCallbacks(String toolName, String inputJson, String expectedSubstring) {
    ToolCallback tool =
        registry.getRegisteredTools().stream()
            .filter(t -> toolName.equals(t.getToolDefinition().name()))
            .findFirst()
            .orElseThrow();

    String responseJson = tool.call(inputJson);
    assertNotNull(responseJson);
    assertTrue(responseJson.contains(expectedSubstring));
  }

  @Test
  void searchWeb_repositoryNull_returnsErrorResponse() {
    DefaultToolRegistry registryNoDb = new DefaultToolRegistry(null, null, null, null);
    registryNoDb.registerDefaultTools();
    ToolCallback tool =
        registryNoDb.getRegisteredTools().stream()
            .filter(t -> "searchWeb".equals(t.getToolDefinition().name()))
            .findFirst()
            .orElseThrow();

    String response = tool.call("{\"query\":\"test\"}");
    assertTrue(response.contains("RAG source repository is not initialized"));
  }

  @Test
  void searchWeb_withMatches_returnsJoinedContent() {
    ToolCallback tool =
        registry.getRegisteredTools().stream()
            .filter(t -> "searchWeb".equals(t.getToolDefinition().name()))
            .findFirst()
            .orElseThrow();

    ToolRagSourceEntity entity = new ToolRagSourceEntity();
    entity.setToolId("searchWeb");
    entity.setContent("Corporate description profile content.");
    entity.setUserId("user-123");

    when(ragSourceRepository.findAll()).thenReturn(List.of(entity));

    // Stub SecurityContextUtil using MockedStatic to return userId: user-123
    try (MockedStatic<SecurityContextUtil> mockSecurity = mockStatic(SecurityContextUtil.class)) {
      mockSecurity
          .when(SecurityContextUtil::extractSecurityMetadata)
          .thenReturn(Map.of("userId", "user-123"));

      String response = tool.call("{\"query\":\"Corporate\"}");
      assertTrue(response.contains("Corporate description profile content"));
    }
  }

  @Test
  void searchWeb_dbFailure_returnsErrorDescription() {
    ToolCallback tool =
        registry.getRegisteredTools().stream()
            .filter(t -> "searchWeb".equals(t.getToolDefinition().name()))
            .findFirst()
            .orElseThrow();

    when(ragSourceRepository.findAll()).thenThrow(new DataRetrievalFailureException("DB error"));

    try (MockedStatic<SecurityContextUtil> mockSecurity = mockStatic(SecurityContextUtil.class)) {
      mockSecurity
          .when(SecurityContextUtil::extractSecurityMetadata)
          .thenReturn(Map.of("userId", "user-123"));

      String response = tool.call("{\"query\":\"Corporate\"}");
      assertTrue(response.contains("Error querying knowledge database"));
    }
  }

  @Test
  void getRegisteredTools_cacheEnabledInConfig_returnsWrappedCallback() {
    PlatformToolConfigProvider.PlatformToolConfig config =
        new PlatformToolConfigProvider.PlatformToolConfig(
            1, "analyzePoster", true, 600, false, null, null);
    when(platformToolConfigProvider.getToolConfig("analyzePoster")).thenReturn(Optional.of(config));

    List<ToolCallback> tools = registry.getRegisteredTools();
    ToolCallback posterTool =
        tools.stream()
            .filter(t -> "analyzePoster".equals(t.getToolDefinition().name()))
            .findFirst()
            .orElseThrow();

    assertTrue(posterTool instanceof CachingToolCallback);
  }

  @Test
  void isRagIngestionEnabled_disabled_returnsFalse() {
    assertFalse(registry.isRagIngestionEnabled());
  }

  @Test
  void isRagIngestionEnabled_enabled_returnsTrue() {
    PlatformToolConfigProvider.PlatformToolConfig config =
        new PlatformToolConfigProvider.PlatformToolConfig(
            1, "analyzePoster", false, 0, true, null, null);
    when(platformToolConfigProvider.getToolConfig("analyzePoster")).thenReturn(Optional.of(config));

    assertTrue(registry.isRagIngestionEnabled());
  }

  @Test
  void triggerIngestion_vectorStoreOrProviderNull_doesNothing() {
    // Vector store is null
    when(knowledgeService.getVectorStore()).thenReturn(null);
    assertDoesNotThrow(registry::triggerIngestion);

    // Restore vector store, config provider returns empty config
    VectorStore mockStore = mock(VectorStore.class);
    when(knowledgeService.getVectorStore()).thenReturn(mockStore);

    DefaultToolRegistry registryNoProvider =
        new DefaultToolRegistry(null, cacheService, ragSourceRepository, knowledgeService);
    registryNoProvider.registerDefaultTools();
    assertDoesNotThrow(registryNoProvider::triggerIngestion);
  }

  @Test
  void triggerIngestion_simpleVectorStoreAndRagEnabled_resetsStatusAndIngests() {
    SimpleVectorStore mockStore = mock(SimpleVectorStore.class);
    when(knowledgeService.getVectorStore()).thenReturn(mockStore);

    ToolRagSourceEntity entity = new ToolRagSourceEntity();
    entity.setId(1L);
    entity.setToolId("analyzePoster");
    entity.setContent("Ingestion content body");
    entity.setMetadata(Map.of("key", "val"));
    entity.setIngested(false);

    when(ragSourceRepository.findAll()).thenReturn(List.of(entity));
    when(ragSourceRepository.findByToolIdAndIngestedFalse("analyzePoster"))
        .thenReturn(List.of(entity));

    PlatformToolConfigProvider.PlatformToolConfig config =
        new PlatformToolConfigProvider.PlatformToolConfig(
            1, "analyzePoster", false, 0, true, "MARKDOWN_CHUNKERS", null);
    when(platformToolConfigProvider.getToolConfig("analyzePoster")).thenReturn(Optional.of(config));

    registry.triggerIngestion();

    verify(ragSourceRepository).saveAll(anyList());
    verify(ragSourceRepository).save(entity);
    verify(mockStore).add(anyList());
    assertTrue(entity.getIngested());
  }

  @Test
  void onApplicationReady_ragDisabled_doesNotTrigger() {
    // Ingestion is disabled since platformToolConfigProvider mock returns empty
    registry.onApplicationReady();
    verify(knowledgeService, never()).getVectorStore();
  }
}
