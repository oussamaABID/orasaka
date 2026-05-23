package com.orasaka.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.orasaka.core.engine.NodeState.Active;
import com.orasaka.core.engine.NodeState.Invisible;
import com.orasaka.core.engine.NodeState.Locked;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests verifying OrasakaGraphEngine compilation and short-circuit logic. */
class OrasakaGraphEngineTest {

  private OrasakaAdminRegistry adminRegistry;

  @BeforeEach
  void setUp() {
    adminRegistry = new OrasakaAdminRegistry();
  }

  @Test
  void testCompileGraphShortCircuit() {
    OrasakaFeaturesProperties.FeatureConfig textFeature =
        new OrasakaFeaturesProperties.FeatureConfig(
            true, "Text Chat", "chat", "/api/v1/chat/stream", "POST", "{}");
    OrasakaFeaturesProperties.FeatureConfig imageFeature =
        new OrasakaFeaturesProperties.FeatureConfig(
            false, "Generate Image", "image", "/api/v1/chat/image", "POST", "{}");

    OrasakaFeaturesProperties properties =
        new OrasakaFeaturesProperties(
            Map.of(
                "orasaka.core.chat.text", textFeature,
                "orasaka.core.chat.image", imageFeature));

    // Lock the disabled feature
    adminRegistry.lock("orasaka.core.chat.image", "Locked by admin");

    OrasakaGraphEngine engine = new OrasakaGraphEngine(properties, adminRegistry);
    OrasakaOperationGraph graph = engine.compileGraph();

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
    OrasakaFeaturesProperties.FeatureConfig textFeature =
        new OrasakaFeaturesProperties.FeatureConfig(
            true, "Text Chat", "chat", "/api/v1/chat/stream", "POST", "{}");

    OrasakaFeaturesProperties properties =
        new OrasakaFeaturesProperties(Map.of("orasaka.core.chat.text", textFeature));

    adminRegistry.lock("orasaka.core.chat.text", "Maintenance mode");

    OrasakaGraphEngine engine = new OrasakaGraphEngine(properties, adminRegistry);
    OrasakaOperationGraph graph = engine.compileGraph();

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
    OrasakaFeaturesProperties.FeatureConfig textConfig =
        new OrasakaFeaturesProperties.FeatureConfig(
            true, "Text Chat", "chat", "/api/v1/chat/stream", "POST", "{\"prompt\":\"${prompt}\"}");
    OrasakaFeaturesProperties.FeatureConfig imageConfig =
        new OrasakaFeaturesProperties.FeatureConfig(
            true,
            "Generate Image",
            "image",
            "/api/v1/chat/image",
            "POST",
            "{\"prompt\":\"${prompt}\"}");
    OrasakaFeaturesProperties.FeatureConfig speechConfig =
        new OrasakaFeaturesProperties.FeatureConfig(
            false,
            "Text to Speech",
            "mic",
            "/api/v1/chat/speech",
            "POST",
            "{\"text\":\"${text}\"}");

    OrasakaFeaturesProperties properties =
        new OrasakaFeaturesProperties(
            Map.of(
                "orasaka.core.chat.text", textConfig,
                "orasaka.core.chat.image", imageConfig,
                "orasaka.core.chat.speech", speechConfig));

    adminRegistry.lock("orasaka.core.chat.image", "Out of API credits");

    OrasakaGraphEngine engine = new OrasakaGraphEngine(properties, adminRegistry);
    OrasakaOperationGraph graph = engine.compileGraph();

    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

    String json = mapper.writeValueAsString(graph);
    System.out.println("=== SAMPLE COMPILED GRAPH JSON ===");
    System.out.println(json);
    System.out.println("==================================");
  }
}
