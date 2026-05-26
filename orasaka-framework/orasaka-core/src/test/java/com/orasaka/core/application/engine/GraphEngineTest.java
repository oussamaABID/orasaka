package com.orasaka.core.application.engine;

import static com.orasaka.test.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orasaka.core.domain.model.NodeState.Active;
import com.orasaka.core.domain.model.NodeState.Invisible;
import com.orasaka.core.domain.model.NodeState.Locked;
import com.orasaka.core.domain.model.OperationGraph;
import com.orasaka.core.domain.model.OperationNode;
import com.orasaka.core.domain.ports.outbound.AdminRegistry;
import com.orasaka.core.domain.ports.outbound.ModelCatalogProvider;
import com.orasaka.core.infrastructure.config.FeaturesProperties;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests verifying GraphEngine compilation and short-circuit logic. */
class GraphEngineTest {

  private AdminRegistry adminRegistry;
  private ModelCatalogProvider modelCatalogProvider;

  @BeforeEach
  void setUp() {
    adminRegistry = new AdminRegistry();
    modelCatalogProvider = Mockito.mock(ModelCatalogProvider.class);
    Mockito.when(modelCatalogProvider.getActiveChatModel()).thenReturn(Optional.of("llama3.1:8b"));
  }

  @Test
  void testCompileGraphShortCircuit() {
    FeaturesProperties.FeatureConfig textFeature =
        new FeaturesProperties.FeatureConfig(
            true, "Text Chat", CHAT, "/api/v1/chat/stream", METHOD_POST, "{}");
    FeaturesProperties.FeatureConfig imageFeature =
        new FeaturesProperties.FeatureConfig(
            false, "Generate Image", "image", "/api/v1/chat/image", METHOD_POST, "{}");

    FeaturesProperties properties =
        new FeaturesProperties(
            Map.of(
                "orasaka.core.chat.text", textFeature,
                "orasaka.core.chat.image", imageFeature));

    // Lock the disabled feature
    adminRegistry.lock("orasaka.core.chat.image", "Locked by admin");

    GraphEngine engine =
        new GraphEngine(
            properties,
            adminRegistry,
            null,
            new com.orasaka.core.application.engine.InfrastructureProber(null, 8188, 8085),
            modelCatalogProvider);
    OperationGraph graph = engine.compileGraph();

    assertThat(graph.nodes()).hasSize(2);

    OperationNode textNode =
        graph.nodes().stream()
            .filter(n -> n.id().equals("orasaka.core.chat.text"))
            .findFirst()
            .orElseThrow();
    OperationNode imageNode =
        graph.nodes().stream()
            .filter(n -> n.id().equals("orasaka.core.chat.image"))
            .findFirst()
            .orElseThrow();

    // Text node is Active
    assertThat(textNode.state()).isInstanceOf(Active.class);

    // Image node is Invisible (short-circuited and ignored lock)
    assertThat(imageNode.state()).isInstanceOf(Invisible.class);
  }

  @Test
  void testCompileGraphAdminLocked() {
    FeaturesProperties.FeatureConfig textFeature =
        new FeaturesProperties.FeatureConfig(
            true, "Text Chat", CHAT, "/api/v1/chat/stream", METHOD_POST, "{}");

    FeaturesProperties properties =
        new FeaturesProperties(Map.of("orasaka.core.chat.text", textFeature));

    adminRegistry.lock("orasaka.core.chat.text", "Maintenance mode");

    GraphEngine engine =
        new GraphEngine(
            properties,
            adminRegistry,
            null,
            new com.orasaka.core.application.engine.InfrastructureProber(null, 8188, 8085),
            modelCatalogProvider);
    OperationGraph graph = engine.compileGraph();

    assertThat(graph.nodes()).hasSize(1);
    OperationNode textNode = graph.nodes().getFirst();

    // State is Locked
    assertThat(textNode.state()).isInstanceOf(Locked.class);
    Locked lockedState = (Locked) textNode.state();
    assertThat(lockedState.reason()).isEqualTo("Maintenance mode");
    assertThat(lockedState.lockedAt()).isNotNull();
  }

  @Test
  void testPrintSampleJsonPayload() throws Exception {
    FeaturesProperties.FeatureConfig textConfig =
        new FeaturesProperties.FeatureConfig(
            true,
            "Text Chat",
            CHAT,
            "/api/v1/chat/stream",
            METHOD_POST,
            "{\"prompt\":\"${prompt}\"}");
    FeaturesProperties.FeatureConfig imageConfig =
        new FeaturesProperties.FeatureConfig(
            true,
            "Generate Image",
            "image",
            "/api/v1/chat/image",
            METHOD_POST,
            "{\"prompt\":\"${prompt}\"}");
    FeaturesProperties.FeatureConfig speechConfig =
        new FeaturesProperties.FeatureConfig(
            false,
            "Text to Speech",
            "mic",
            "/api/v1/chat/speech",
            METHOD_POST,
            "{\"text\":\"${text}\"}");

    FeaturesProperties properties =
        new FeaturesProperties(
            Map.of(
                "orasaka.core.chat.text", textConfig,
                "orasaka.core.chat.image", imageConfig,
                "orasaka.core.chat.speech", speechConfig));

    adminRegistry.lock("orasaka.core.chat.image", "Out of API credits");

    GraphEngine engine =
        new GraphEngine(
            properties,
            adminRegistry,
            null,
            new com.orasaka.core.application.engine.InfrastructureProber(null, 8188, 8085),
            modelCatalogProvider);
    OperationGraph graph = engine.compileGraph();

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    String json = mapper.writeValueAsString(graph);

    // Verify serializable and structurally sound
    assertThat(json).isNotBlank().contains("\"nodes\"");
    assertThat(graph.nodes()).hasSize(3);
  }

  @Test
  void testCompileGraphActiveModelNull() {
    FeaturesProperties.FeatureConfig textFeature =
        new FeaturesProperties.FeatureConfig(
            true, "Text Chat", CHAT, "/api/v1/chat/stream", METHOD_POST, "{}");
    FeaturesProperties properties =
        new FeaturesProperties(Map.of("orasaka.core.chat.text", textFeature));
    Mockito.when(modelCatalogProvider.getActiveChatModel()).thenReturn(Optional.empty());

    GraphEngine engine =
        new GraphEngine(
            properties,
            adminRegistry,
            null,
            Mockito.mock(InfrastructureProber.class),
            modelCatalogProvider);
    OperationGraph graph = engine.compileGraph();

    assertThat(graph.nodes()).hasSize(1);
    assertThat(graph.nodes().get(0).state()).isInstanceOf(Locked.class);
    assertThat(((Locked) graph.nodes().get(0).state()).reason()).contains("Capability missing");
  }

  @Test
  void testCompileGraphVideoFeatureProbing() {
    FeaturesProperties.FeatureConfig videoFeature =
        new FeaturesProperties.FeatureConfig(
            true, "Video Gen", "video", "/api/v1/video/generate", METHOD_POST, "{}");
    FeaturesProperties properties =
        new FeaturesProperties(Map.of("orasaka.core.media.video", videoFeature));

    InfrastructureProber mockProber = Mockito.mock(InfrastructureProber.class);
    // Case 1: Both offline
    Mockito.when(mockProber.isVideoEngineOnline()).thenReturn(false);
    Mockito.when(mockProber.isImageEngineOnline()).thenReturn(false);
    Mockito.when(mockProber.getVideoProbePort()).thenReturn(8188);
    Mockito.when(mockProber.getImageProbePort()).thenReturn(8085);

    GraphEngine engine =
        new GraphEngine(properties, adminRegistry, null, mockProber, modelCatalogProvider);
    OperationGraph graph = engine.compileGraph();
    assertThat(((Locked) graph.nodes().get(0).state()).reason())
        .contains("Video engine offline on port 8188");

    // Case 2: Video online
    engine.invalidateCache();
    Mockito.when(mockProber.isVideoEngineOnline()).thenReturn(true);
    OperationGraph graph2 = engine.compileGraph();
    assertThat(graph2.nodes().get(0).state()).isInstanceOf(Active.class);

    // Case 3: Image online (fallback)
    engine.invalidateCache();
    Mockito.when(mockProber.isVideoEngineOnline()).thenReturn(false);
    Mockito.when(mockProber.isImageEngineOnline()).thenReturn(true);
    OperationGraph graph3 = engine.compileGraph();
    assertThat(graph3.nodes().get(0).state()).isInstanceOf(Active.class);
  }
}
