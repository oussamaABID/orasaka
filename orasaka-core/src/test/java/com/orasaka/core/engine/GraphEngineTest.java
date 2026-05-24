package com.orasaka.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orasaka.core.engine.NodeState.Active;
import com.orasaka.core.engine.NodeState.Invisible;
import com.orasaka.core.engine.NodeState.Locked;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests verifying GraphEngine compilation and short-circuit logic. */
class GraphEngineTest {

  private AdminRegistry adminRegistry;

  @BeforeEach
  void setUp() {
    adminRegistry = new AdminRegistry();
  }

  @Test
  void testCompileGraphShortCircuit() {
    FeaturesProperties.FeatureConfig textFeature =
        new FeaturesProperties.FeatureConfig(
            true, "Text Chat", "chat", "/api/v1/chat/stream", "POST", "{}");
    FeaturesProperties.FeatureConfig imageFeature =
        new FeaturesProperties.FeatureConfig(
            false, "Generate Image", "image", "/api/v1/chat/image", "POST", "{}");

    FeaturesProperties properties =
        new FeaturesProperties(
            Map.of(
                "orasaka.core.chat.text", textFeature,
                "orasaka.core.chat.image", imageFeature));

    // Lock the disabled feature
    adminRegistry.lock("orasaka.core.chat.image", "Locked by admin");

    GraphEngine engine = new GraphEngine(properties, adminRegistry);
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
            true, "Text Chat", "chat", "/api/v1/chat/stream", "POST", "{}");

    FeaturesProperties properties =
        new FeaturesProperties(Map.of("orasaka.core.chat.text", textFeature));

    adminRegistry.lock("orasaka.core.chat.text", "Maintenance mode");

    GraphEngine engine = new GraphEngine(properties, adminRegistry);
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
            true, "Text Chat", "chat", "/api/v1/chat/stream", "POST", "{\"prompt\":\"${prompt}\"}");
    FeaturesProperties.FeatureConfig imageConfig =
        new FeaturesProperties.FeatureConfig(
            true,
            "Generate Image",
            "image",
            "/api/v1/chat/image",
            "POST",
            "{\"prompt\":\"${prompt}\"}");
    FeaturesProperties.FeatureConfig speechConfig =
        new FeaturesProperties.FeatureConfig(
            false,
            "Text to Speech",
            "mic",
            "/api/v1/chat/speech",
            "POST",
            "{\"text\":\"${text}\"}");

    FeaturesProperties properties =
        new FeaturesProperties(
            Map.of(
                "orasaka.core.chat.text", textConfig,
                "orasaka.core.chat.image", imageConfig,
                "orasaka.core.chat.speech", speechConfig));

    adminRegistry.lock("orasaka.core.chat.image", "Out of API credits");

    GraphEngine engine = new GraphEngine(properties, adminRegistry);
    OperationGraph graph = engine.compileGraph();

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    String json = mapper.writeValueAsString(graph);
    System.out.println("=== SAMPLE COMPILED GRAPH JSON ===");
    System.out.println(json);
    System.out.println("==================================");
  }
}
